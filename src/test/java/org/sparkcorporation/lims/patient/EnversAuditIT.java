package org.sparkcorporation.lims.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sparkcorporation.lims.patient.PatientBuilder.aPatient;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.iam.infra.LimsRevisionEntity;
import org.sparkcorporation.lims.patient.domain.Patient;
import org.sparkcorporation.lims.patient.domain.UpdatePatient;
import org.sparkcorporation.lims.patient.infra.PatientRepository;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Envers row-level history smoke IT (SP8 chunk 4). Proves the audit wiring end-to-end against the
 * Testcontainers DB: an insert + an update on an {@code @Audited} {@link Patient} each produce a
 * {@code patients_aud} revision, the acting user id is stamped onto {@code revinfo.user_id} from
 * the security context, and an anonymous (no-auth) write records a {@code null} user id.
 *
 * <p>Each write runs in its own committed transaction ({@link TransactionTemplate}) because Envers
 * flushes {@code _aud} rows on commit; a rolled-back {@code @DataJpaTest} slice would record
 * nothing.
 */
@SpringBootTest
class EnversAuditIT extends IntegrationTestBase {

    @Autowired private PatientRepository patients;
    @Autowired private TransactionTemplate tx;
    @Autowired private EntityManager em;

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void insert_and_update_record_revisions_with_acting_user() {
        String actingUser = Ulid.generate();
        authenticateAs(actingUser);

        Patient created =
                tx.execute(
                        status -> {
                            Patient p =
                                    aPatient()
                                            .withName("AUDIT", "Subject")
                                            .withLabCode(uniqueLabCode())
                                            .build();
                            return patients.saveAndFlush(p);
                        });
        String patientId = created.id().value();

        tx.executeWithoutResult(
                status -> {
                    Patient p = patients.findById(patientId).orElseThrow();
                    p.update(
                            new UpdatePatient(
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    "Biologist",
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null));
                    patients.saveAndFlush(p);
                });

        List<Number> revisions = revisionsOf(patientId);
        assertThat(revisions).hasSize(2);
        revisions.forEach(
                rev -> {
                    Patient snapshot = snapshotAt(patientId, rev);
                    assertThat(snapshot).isNotNull();
                    assertThat(revisionUserId(rev)).isEqualTo(actingUser);
                });
    }

    @Test
    void anonymous_write_records_null_user_id() {
        SecurityContextHolder.clearContext();

        Patient created =
                tx.execute(
                        status -> {
                            Patient p =
                                    aPatient()
                                            .withName("ANON", "Subject")
                                            .withLabCode(uniqueLabCode())
                                            .build();
                            return patients.saveAndFlush(p);
                        });
        String patientId = created.id().value();

        List<Number> revisions = revisionsOf(patientId);
        assertThat(revisions).hasSize(1);
        assertThat(revisionUserId(revisions.get(0))).isNull();
    }

    private void authenticateAs(String userId) {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    private AuditReader auditReader() {
        return AuditReaderFactory.get(em);
    }

    @SuppressWarnings("unchecked")
    private List<Number> revisionsOf(String patientId) {
        return tx.execute(status -> auditReader().getRevisions(Patient.class, patientId));
    }

    private Patient snapshotAt(String patientId, Number rev) {
        return tx.execute(status -> auditReader().find(Patient.class, patientId, rev));
    }

    private String revisionUserId(Number rev) {
        return tx.execute(
                status -> auditReader().findRevision(LimsRevisionEntity.class, rev).getUserId());
    }

    /**
     * Unique, format-valid lab code ({@code AUD-260611-NNNNN}). Draws from the suite-wide {@code
     * REG_SEQ} so concurrent IT classes against the shared container never collide on the unique
     * {@code lab_code} column.
     */
    private String uniqueLabCode() {
        return String.format("AUD-260611-%05d", REG_SEQ.getAndIncrement());
    }
}
