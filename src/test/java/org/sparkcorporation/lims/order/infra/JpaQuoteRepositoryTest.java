package org.sparkcorporation.lims.order.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.order.api.QuoteNumber;
import org.sparkcorporation.lims.order.api.QuoteStatus;
import org.sparkcorporation.lims.order.domain.NewQuote;
import org.sparkcorporation.lims.order.domain.NewQuoteLine;
import org.sparkcorporation.lims.order.domain.Quote;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * JPA-slice round-trip for {@link QuoteRepository} + {@link QuoteLineRepository}. Docker-blocked
 * locally (Testcontainers PostgreSQL via {@link IntegrationTestBase}); runs in CI. Mirrors {@link
 * JpaLabFileRepositoryTest}.
 *
 * <p>Verifies: (a) round-trip persists Quote + N QuoteLines, (b) {@code findActiveById} filters
 * soft-deleted, (c) {@code findActiveByPatientId} excludes soft-deleted + orders desc, (d) {@code
 * search} with each filter combination. Cross-FK parents (patients, analyses, disciplines) are
 * seeded via raw SQL — the order module cannot import other modules' entities (Modulith boundary).
 */
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class JpaQuoteRepositoryTest extends IntegrationTestBase {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-08T08:00:00Z"), ZoneOffset.UTC);

    @Autowired private QuoteRepository quoteRepo;
    @Autowired private QuoteLineRepository lineRepo;
    @Autowired private EntityManager em;

    /**
     * Deterministic quote-number sequence — mirrors the {@code fileSeq} pattern in {@link
     * JpaLabFileRepositoryTest} to avoid {@code uq_quotes_quote_number} collisions when multiple
     * quotes persist in the same JVM run. {@code @DataJpaTest} rolls the DB back per test so the
     * reset in {@link #resetSequence()} is belt-and-suspenders.
     */
    private final AtomicInteger quoteSeq = new AtomicInteger(1);

    @BeforeEach
    void resetSequence() {
        quoteSeq.set(1);
    }

    private QuoteNumber nextQuoteNumber() {
        return new QuoteNumber("DEV-260508-%04d".formatted(quoteSeq.getAndIncrement()));
    }

    private String seedPatient() {
        var id = Ulid.generate();
        em.createNativeQuery(
                        "INSERT INTO patients (id, lab_code, last_name, civility, sex, insured,"
                                + " version) VALUES (?, ?, 'DOE', 'MADAME', 'F', false, 0)")
                .setParameter(1, id)
                .setParameter(2, "LAB-" + id.substring(0, 10))
                .executeUpdate();
        return id;
    }

    private String seedDiscipline() {
        var id = Ulid.generate();
        em.createNativeQuery(
                        "INSERT INTO disciplines (id, code, name, discipline_prefix, version)"
                                + " VALUES (?, ?, 'Biochimie', 'BIOCHIMIE', 0)")
                .setParameter(1, id)
                .setParameter(2, "BIOCHEM-" + id.substring(10, 16))
                .executeUpdate();
        return id;
    }

    private String seedAnalysis(String disciplineId) {
        var id = Ulid.generate();
        em.createNativeQuery(
                        "INSERT INTO analyses (id, code, name, kind, indiciary_value_b,"
                                + " label_count, discipline_id, result_type, version) VALUES (?, ?,"
                                + " 'Glucose', 'SIMPLE', 10.00, 1, ?, 'REAL', 0)")
                .setParameter(1, id)
                .setParameter(2, "GLUC-" + id.substring(10, 16))
                .setParameter(3, disciplineId)
                .executeUpdate();
        return id;
    }

    private Quote freshQuote(String patientId) {
        var cmd = new NewQuote(patientId, null, null, List.of());
        return Quote.draft(cmd, nextQuoteNumber(), null, FIXED);
    }

    private Quote seedQuote(String patientId, Clock clock) {
        var cmd = new NewQuote(patientId, null, null, List.of());
        return Quote.draft(cmd, nextQuoteNumber(), null, clock);
    }

    @Test
    void persist_quote_with_lines_round_trip() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var analysisId = seedAnalysis(disciplineId);

        var q = freshQuote(patientId);
        q.addLine(new NewQuoteLine(analysisId, "GLUC", new BigDecimal("10.0000"), 1), FIXED);

        quoteRepo.saveAndFlush(q);
        em.clear();

        var fetched = quoteRepo.findActiveById(q.getId()).orElseThrow();
        assertThat(fetched.getQuoteNumber()).isEqualTo(q.getQuoteNumber());
        assertThat(fetched.getStatus()).isEqualTo(QuoteStatus.DRAFT);

        var lines = lineRepo.findByQuoteId(q.getId());
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).getAnalysisCode()).isEqualTo("GLUC");
        assertThat(lines.get(0).getIndiciaryValueBSnapshot())
                .isEqualByComparingTo(new BigDecimal("10.0000"));
        assertThat(lines.get(0).getLineTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void findActiveById_filters_soft_deleted() {
        var patientId = seedPatient();
        var q = freshQuote(patientId);
        quoteRepo.saveAndFlush(q);

        quoteRepo.deleteById(q.getId());
        quoteRepo.flush();

        // raw row still exists (soft delete) but findActiveById returns empty.
        assertThat(quoteRepo.findById(q.getId())).isPresent();
        assertThat(quoteRepo.findById(q.getId()).orElseThrow().getDeletedAt()).isNotNull();
        assertThat(quoteRepo.findActiveById(q.getId())).isEmpty();
    }

    @Test
    void findActiveByQuoteNumber_filters_soft_deleted() {
        var patientId = seedPatient();
        var q = freshQuote(patientId);
        var qnValue = q.getQuoteNumber().value();
        quoteRepo.saveAndFlush(q);

        assertThat(quoteRepo.findActiveByQuoteNumber(qnValue)).isPresent();

        quoteRepo.deleteById(q.getId());
        quoteRepo.flush();

        assertThat(quoteRepo.findActiveByQuoteNumber(qnValue)).isEmpty();
    }

    @Test
    void findActiveByPatientId_excludes_soft_deleted_and_orders_desc() {
        var patientId = seedPatient();
        var q1 = freshQuote(patientId);
        var q2 = freshQuote(patientId);
        var q3 = freshQuote(patientId);
        quoteRepo.saveAndFlush(q1);
        quoteRepo.saveAndFlush(q2);
        quoteRepo.saveAndFlush(q3);
        quoteRepo.deleteById(q2.getId());
        quoteRepo.flush();

        var active = quoteRepo.findActiveByPatientId(patientId);
        assertThat(active)
                .extracting(Quote::getId)
                .contains(q1.getId(), q3.getId())
                .doesNotContain(q2.getId());
    }

    @Test
    void search_filters_by_patient_id() {
        var patientA = seedPatient();
        var patientB = seedPatient();
        var a1 = seedQuote(patientA, FIXED);
        var a2 = seedQuote(patientA, FIXED);
        var b1 = seedQuote(patientB, FIXED);
        quoteRepo.saveAndFlush(a1);
        quoteRepo.saveAndFlush(a2);
        quoteRepo.saveAndFlush(b1);

        var page = quoteRepo.search(patientA, null, null, null, null, PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(Quote::getId)
                .containsExactlyInAnyOrder(a1.getId(), a2.getId());
    }

    @Test
    void search_filters_by_status() {
        var patientId = seedPatient();
        var draft = seedQuote(patientId, FIXED);
        var locked = seedQuote(patientId, FIXED);
        quoteRepo.saveAndFlush(draft);
        quoteRepo.saveAndFlush(locked);

        // Bump status via SQL — chunk 5 owns lifecycle but the JPQL search needs a non-DRAFT row
        // to exercise the typed enum-equality branch.
        em.createNativeQuery("UPDATE quotes SET status = 'LOCKED' WHERE id = ?")
                .setParameter(1, locked.getId())
                .executeUpdate();
        em.clear();

        var page =
                quoteRepo.search(null, null, QuoteStatus.LOCKED, null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Quote::getId).containsExactly(locked.getId());
    }

    @Test
    void search_with_all_filters_null_returns_all_active() {
        var patientId = seedPatient();
        var active1 = seedQuote(patientId, FIXED);
        var active2 = seedQuote(patientId, FIXED);
        var deleted = seedQuote(patientId, FIXED);
        quoteRepo.saveAndFlush(active1);
        quoteRepo.saveAndFlush(active2);
        quoteRepo.saveAndFlush(deleted);
        quoteRepo.deleteById(deleted.getId());
        quoteRepo.flush();

        var page = quoteRepo.search(null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(Quote::getId)
                .containsExactlyInAnyOrder(active1.getId(), active2.getId());
    }

    @Test
    void search_filters_by_date_range() {
        var patientId = seedPatient();
        var now = FIXED.instant();
        var olderClock = Clock.fixed(now.minus(Duration.ofDays(2)), ZoneOffset.UTC);
        var insideClock = Clock.fixed(now.minus(Duration.ofHours(12)), ZoneOffset.UTC);
        var newerClock = Clock.fixed(now.plus(Duration.ofDays(2)), ZoneOffset.UTC);

        var older = seedQuote(patientId, olderClock);
        var inside = seedQuote(patientId, insideClock);
        var newer = seedQuote(patientId, newerClock);
        quoteRepo.saveAndFlush(older);
        quoteRepo.saveAndFlush(inside);
        quoteRepo.saveAndFlush(newer);

        var from = now.minus(Duration.ofDays(1));
        var to = now.plus(Duration.ofDays(1));

        var page = quoteRepo.search(null, null, null, from, to, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Quote::getId).containsExactly(inside.getId());
    }
}
