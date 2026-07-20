package org.sparkcorporation.lims.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.ReferenceMode;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.catalog.domain.Analysis;
import org.sparkcorporation.lims.catalog.domain.AnalysisTechniqueLink;
import org.sparkcorporation.lims.catalog.domain.Discipline;
import org.sparkcorporation.lims.catalog.domain.NewAnalysis;
import org.sparkcorporation.lims.catalog.domain.NewReferenceInterval;
import org.sparkcorporation.lims.catalog.domain.ReferenceInterval;
import org.sparkcorporation.lims.catalog.domain.Technique;
import org.sparkcorporation.lims.catalog.domain.UpdateAnalysis;
import org.sparkcorporation.lims.catalog.infra.AnalysisRepository;
import org.sparkcorporation.lims.catalog.infra.DisciplineRepository;
import org.sparkcorporation.lims.catalog.infra.ReferenceIntervalRepository;
import org.sparkcorporation.lims.catalog.infra.TechniqueRepository;
import org.sparkcorporation.lims.catalog.web.AnalysisHistoryEntry;
import org.sparkcorporation.lims.catalog.web.AnalysisHistoryEntry.Scope;
import org.sparkcorporation.lims.catalog.web.AnalysisHistoryResponse;
import org.sparkcorporation.lims.catalog.web.AnalysisHistoryService;
import org.sparkcorporation.lims.catalog.web.ReferenceIntervalService;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * SP12 Envers history smoke IT for the catalog config aggregate. Proves that config mutations on an
 * {@code @Audited} analysis and its sub-aggregates produce audit revisions against the
 * Testcontainers DB.
 */
@SpringBootTest
class AnalysisConfigHistoryIT extends IntegrationTestBase {

    @Autowired private AnalysisRepository analyses;
    @Autowired private DisciplineRepository disciplines;
    @Autowired private ReferenceIntervalRepository intervals;
    @Autowired private ReferenceIntervalService intervalService;
    @Autowired private AnalysisHistoryService historyService;
    @Autowired private TechniqueRepository techniqueRepo;
    @Autowired private TransactionTemplate tx;
    @Autowired private EntityManager em;

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void replicate_records_delete_and_add_revisions_for_target_category_only() {
        String user = Ulid.generate();
        authenticateAs(user);

        AnalysisId aid = seedSimpleAnalysis();
        // Source (HOMME) interval + a pre-existing target (FEMME) interval that replicate must
        // soft-delete (→ Envers DEL) before re-cloning the source into FEMME (→ Envers ADD).
        tx.executeWithoutResult(
                s -> {
                    intervals.saveAndFlush(standard(aid, PatientCategory.HOMME));
                    intervals.saveAndFlush(standard(aid, PatientCategory.FEMME));
                });

        intervalService.replicate(
                aid, PatientCategory.HOMME, java.util.Set.of(PatientCategory.FEMME));

        List<RevisionType> revtypes = intervalRevisionTypes(aid);
        assertThat(revtypes).contains(RevisionType.DEL, RevisionType.ADD);

        // The non-target source category (HOMME) is never soft-deleted by replicate.
        tx.executeWithoutResult(
                s ->
                        assertThat(
                                        intervals.findActiveByAnalysisIdAndCategory(
                                                aid.value(), PatientCategory.HOMME))
                                .hasSize(1));
    }

    @Test
    void field_update_records_changed_field_and_acting_user() {
        String user = Ulid.generate();
        authenticateAs(user);

        AnalysisId aid = seedSimpleAnalysis();
        tx.executeWithoutResult(
                s -> {
                    Analysis a = analyses.findById(aid.value()).orElseThrow();
                    a.update(
                            new UpdateAnalysis(
                                    "Renamed analysis",
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null),
                            null,
                            null,
                            Clock.systemUTC());
                    analyses.saveAndFlush(a);
                });

        AnalysisHistoryResponse history = historyService.history(aid, 0, 50);
        AnalysisHistoryEntry mod =
                history.content().stream()
                        .filter(e -> e.scope() == Scope.ANALYSIS)
                        .filter(e -> e.changedFields().contains("name"))
                        .findFirst()
                        .orElseThrow(
                                () -> new AssertionError("no ANALYSIS entry with name change"));
        assertThat(mod.userId()).isEqualTo(user);
    }

    @Test
    void adding_a_component_records_a_components_entry_with_acting_user() {
        String user = Ulid.generate();
        authenticateAs(user);

        AnalysisId parentId = seedGroupedParentWithChild();

        AnalysisHistoryResponse history = historyService.history(parentId, 0, 50);
        AnalysisHistoryEntry components =
                history.content().stream()
                        .filter(e -> e.scope() == Scope.COMPONENTS)
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("no COMPONENTS entry"));
        assertThat(components.userId()).isEqualTo(user);
        assertThat(components.changedFields())
                .anySatisfy(f -> assertThat(f).contains("components"));
    }

    @Test
    void adding_a_technique_records_a_techniques_entry_with_acting_user() {
        String user = Ulid.generate();
        authenticateAs(user);

        AnalysisId aid = seedSimpleAnalysis();
        tx.executeWithoutResult(
                s -> {
                    String suffix = String.format("%05d", REG_SEQ.getAndIncrement());
                    Technique t =
                            techniqueRepo.saveAndFlush(
                                    Technique.create("TECH" + suffix, "Technique " + suffix, null));
                    Analysis a = analyses.findById(aid.value()).orElseThrow();
                    a.techniques().add(new AnalysisTechniqueLink(a.id(), t.id(), true));
                    analyses.saveAndFlush(a);
                });

        AnalysisHistoryResponse history = historyService.history(aid, 0, 50);
        AnalysisHistoryEntry techniques =
                history.content().stream()
                        .filter(e -> e.scope() == Scope.TECHNIQUES)
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("no TECHNIQUES entry"));
        assertThat(techniques.userId()).isEqualTo(user);
    }

    private AnalysisId seedGroupedParentWithChild() {
        return tx.execute(
                s -> {
                    String suffix = String.format("%05d", REG_SEQ.getAndIncrement());
                    Discipline d =
                            disciplines.save(
                                    Discipline.create(
                                            "HCMPD" + suffix,
                                            "D" + suffix,
                                            null,
                                            DisciplinePrefix.BIOCHIMIE));
                    Analysis child =
                            analyses.saveAndFlush(
                                    Analysis.create(
                                            simpleCmd("HCMPC" + suffix, d.id()),
                                            d,
                                            null,
                                            null,
                                            Clock.systemUTC()));
                    Analysis parent =
                            Analysis.create(
                                    new NewAnalysis(
                                            "HCMPP" + suffix,
                                            "Grouped parent " + suffix,
                                            AnalysisKind.GROUPED,
                                            new BigDecimal("20.00"),
                                            1,
                                            d.id(),
                                            null,
                                            null,
                                            ResultType.REAL,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null),
                                    d,
                                    null,
                                    null,
                                    Clock.systemUTC());
                    parent.addComponent(child, 0);
                    return analyses.saveAndFlush(parent).id();
                });
    }

    private static NewAnalysis simpleCmd(
            String code, org.sparkcorporation.lims.catalog.api.DisciplineId disciplineId) {
        return new NewAnalysis(
                code,
                "Child " + code,
                AnalysisKind.SIMPLE,
                new BigDecimal("10.00"),
                1,
                disciplineId,
                null,
                null,
                ResultType.REAL,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private AnalysisId seedSimpleAnalysis() {
        return tx.execute(
                s -> {
                    String suffix = String.format("%05d", REG_SEQ.getAndIncrement());
                    Discipline d =
                            disciplines.save(
                                    Discipline.create(
                                            "HISTD" + suffix,
                                            "D" + suffix,
                                            null,
                                            DisciplinePrefix.BIOCHIMIE));
                    NewAnalysis cmd =
                            new NewAnalysis(
                                    "HISTA" + suffix,
                                    "History analysis " + suffix,
                                    AnalysisKind.SIMPLE,
                                    new BigDecimal("10.00"),
                                    1,
                                    d.id(),
                                    null,
                                    null,
                                    ResultType.REAL,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null);
                    Analysis a =
                            analyses.saveAndFlush(
                                    Analysis.create(cmd, d, null, null, Clock.systemUTC()));
                    return a.id();
                });
    }

    private static ReferenceInterval standard(AnalysisId aid, PatientCategory cat) {
        NewReferenceInterval cmd =
                new NewReferenceInterval(
                        cat,
                        null,
                        null,
                        null,
                        null,
                        ReferenceMode.STANDARD,
                        new BigDecimal("3.5"),
                        new BigDecimal("5.5"),
                        null,
                        null,
                        null);
        return ReferenceInterval.create(cmd, aid, Clock.systemUTC());
    }

    @SuppressWarnings("unchecked")
    private List<RevisionType> intervalRevisionTypes(AnalysisId aid) {
        return tx.execute(
                s -> {
                    AuditReader reader = AuditReaderFactory.get(em);
                    List<Object[]> rows =
                            reader.createQuery()
                                    .forRevisionsOfEntity(ReferenceInterval.class, false, true)
                                    .add(AuditEntity.property("analysisId").eq(aid.value()))
                                    .getResultList();
                    return rows.stream().map(r -> (RevisionType) r[2]).toList();
                });
    }

    private void authenticateAs(String userId) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }
}
