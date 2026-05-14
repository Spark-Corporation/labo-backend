package org.sparkcorporation.lims.order.infra;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.SampleDailyId;
import org.sparkcorporation.lims.order.domain.ExternalReceivedSample;
import org.sparkcorporation.lims.order.domain.HomeSample;
import org.sparkcorporation.lims.order.domain.InLabSample;
import org.sparkcorporation.lims.order.domain.LabFile;
import org.sparkcorporation.lims.order.domain.NewAnalysisRequest;
import org.sparkcorporation.lims.order.domain.NewLabFile;
import org.sparkcorporation.lims.order.domain.Sample;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * JPA-slice round-trip for {@link LabFileRepository} + {@link SampleRepository} + {@link
 * AnalysisRequestRepository}. Docker-blocked locally (Testcontainers PostgreSQL via {@link
 * IntegrationTestBase}); runs in CI.
 *
 * <p>Verifies: (a) the {@code sample_type} discriminator persists for each sealed sub-type, (b)
 * {@code findActiveById} filters soft-deleted lab files, (c) the multi-bag-fetch split works (no
 * {@code MultipleBagFetchException} when both samples + analyses bags are populated). Cross-FK
 * parents (patients, prescribers, preleveurs, analyses, recipients, disciplines) are seeded via raw
 * SQL — the order module cannot import other modules' entities (Modulith boundary).
 */
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class JpaLabFileRepositoryTest extends IntegrationTestBase {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-07T08:00:00Z"), ZoneOffset.UTC);

    @Autowired private LabFileRepository fileRepo;
    @Autowired private SampleRepository sampleRepo;
    @Autowired private AnalysisRequestRepository requestRepo;
    @Autowired private EntityManager em;

    /**
     * Deterministic registration-number sequence — replaces a {@code Math.random()} 4-digit suffix
     * that risked collisions against {@code lab_files.registration_number UNIQUE NOT NULL} when
     * multiple lab files persist in the same JVM run. {@code @DataJpaTest} rolls the DB back per
     * test so the reset in {@link #resetSequence()} is belt-and-suspenders.
     */
    private final AtomicInteger fileSeq = new AtomicInteger(1);

    @BeforeEach
    void resetSequence() {
        fileSeq.set(1);
    }

    private String nextRegistrationNumber() {
        return "LF-260507-%04d".formatted(fileSeq.getAndIncrement());
    }

    /** Inserts a minimum row in {@code patients} so the FK constraint holds. */
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
                                + " VALUES (?, ?, 'Biochimie', '1', 0)")
                .setParameter(1, id)
                .setParameter(2, "BIOCHEM-" + id.substring(10, 16))
                .executeUpdate();
        return id;
    }

    private String seedRecipient() {
        var id = Ulid.generate();
        em.createNativeQuery(
                        "INSERT INTO recipients (id, code, name, active, version) VALUES (?, ?,"
                                + " 'Tube EDTA', true, 0)")
                .setParameter(1, id)
                .setParameter(2, "TUBE-" + id.substring(10, 16))
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

    private String seedPreleveur() {
        var id = Ulid.generate();
        em.createNativeQuery(
                        "INSERT INTO preleveurs (id, code, name, version) VALUES (?, ?, 'Préleveur"
                                + " Test', 0)")
                .setParameter(1, id)
                .setParameter(2, "PRL-" + id.substring(10, 16))
                .executeUpdate();
        return id;
    }

    private LabFile freshFile(String patientId) {
        var cmd =
                new NewLabFile(
                        patientId,
                        null,
                        null,
                        false,
                        List.of("GLUC"),
                        null,
                        null,
                        java.math.BigDecimal.ZERO,
                        java.math.BigDecimal.ZERO,
                        null,
                        null);
        var rn = new LabFileRegistrationNumber(nextRegistrationNumber());
        return LabFile.open(cmd, rn, null, FIXED);
    }

    /**
     * Builds a {@link LabFile} with the supplied flags + clock for the {@link
     * LabFileRepository#search} tests below. {@code prescriberId} is left {@code null} (chunk 4
     * scope: tests of the search method need only patient/urgent/status/date branches; the
     * prescriber branch follows the same JPQL pattern and is exercised indirectly via the {@code
     * all-filters-null} case).
     */
    private LabFile seedLabFile(String patientId, boolean urgent, Clock clock) {
        var cmd =
                new NewLabFile(
                        patientId,
                        null,
                        null,
                        urgent,
                        List.of("GLUC"),
                        null,
                        null,
                        java.math.BigDecimal.ZERO,
                        java.math.BigDecimal.ZERO,
                        null,
                        null);
        var rn = new LabFileRegistrationNumber(nextRegistrationNumber());
        return LabFile.open(cmd, rn, null, clock);
    }

    /**
     * Reflectively wires the package-private back-ref + dailyId on a {@link Sample} sub-type. In
     * production the {@code LabFileService} (chunk 5) does this via the package-private {@code
     * attachToFile(...)}; tests in {@code order.infra} cannot reach it directly, so we use
     * reflection on the protected {@code labFile} / {@code dailyId} fields declared in {@link
     * Sample}.
     */
    private static void attach(Sample s, LabFile file, SampleDailyId dailyId) {
        try {
            var labFileField = Sample.class.getDeclaredField("labFile");
            labFileField.setAccessible(true);
            labFileField.set(s, file);
            var dailyIdField = Sample.class.getDeclaredField("dailyId");
            dailyIdField.setAccessible(true);
            dailyIdField.set(s, dailyId);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("failed to attach sample for test", e);
        }
    }

    @Test
    void persist_lab_file_with_in_lab_sample_round_trip() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var analysisId = seedAnalysis(disciplineId);
        var recipientId = seedRecipient();

        var file = freshFile(patientId);
        var sample = InLabSample.create(analysisId, recipientId, FIXED.instant(), Ulid.generate());
        attach(sample, file, new SampleDailyId("1070526-001"));
        file.addSample(sample, FIXED);

        fileRepo.saveAndFlush(file);
        em.clear();

        var fetched = fileRepo.findActiveById(file.getId()).orElseThrow();
        assertThat(fetched.getRegistrationNumber()).isEqualTo(file.getRegistrationNumber());

        var samples = sampleRepo.findByLabFileId(file.getId());
        assertThat(samples).hasSize(1);
        assertThat(samples.get(0)).isInstanceOf(InLabSample.class);
    }

    @Test
    void persist_home_and_external_received_sample_discriminators() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var analysisId = seedAnalysis(disciplineId);
        var recipientId = seedRecipient();
        var preleveurId = seedPreleveur();

        var file = freshFile(patientId);
        var home =
                HomeSample.create(
                        analysisId,
                        recipientId,
                        FIXED.instant(),
                        Ulid.generate(),
                        "12 rue X",
                        5,
                        FIXED.instant(),
                        FIXED.instant().plusSeconds(3600),
                        preleveurId);
        attach(home, file, new SampleDailyId("1070526-002"));
        file.addSample(home, FIXED);

        var ext =
                ExternalReceivedSample.create(
                        analysisId,
                        recipientId,
                        null,
                        FIXED.instant(),
                        Ulid.generate(),
                        "Clinique partenaire");
        attach(ext, file, new SampleDailyId("1070526-003"));
        file.addSample(ext, FIXED);

        fileRepo.saveAndFlush(file);
        em.clear();

        var samples = sampleRepo.findByLabFileId(file.getId());
        assertThat(samples).hasSize(2);
        assertThat(samples)
                .extracting(Sample::getClass)
                .containsExactlyInAnyOrder(HomeSample.class, ExternalReceivedSample.class);
    }

    @Test
    void persist_analysis_requests_round_trip() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var analysisId = seedAnalysis(disciplineId);

        var file = freshFile(patientId);
        file.addAnalysisRequest(new NewAnalysisRequest(analysisId, "GLUC"), FIXED);

        fileRepo.saveAndFlush(file);
        em.clear();

        var requests = requestRepo.findByLabFileId(file.getId());
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getAnalysisCode()).isEqualTo("GLUC");
        assertThat(requestRepo.existsByLabFileIdAndAnalysisId(file.getId(), analysisId)).isTrue();
        assertThat(requestRepo.existsByLabFileIdAndAnalysisId(file.getId(), Ulid.generate()))
                .isFalse();
    }

    @Test
    void multi_bag_fetch_split_avoids_MultipleBagFetchException() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var analysisId = seedAnalysis(disciplineId);
        var recipientId = seedRecipient();

        var file = freshFile(patientId);
        var sample = InLabSample.create(analysisId, recipientId, FIXED.instant(), Ulid.generate());
        attach(sample, file, new SampleDailyId("1070526-004"));
        file.addSample(sample, FIXED);
        file.addAnalysisRequest(new NewAnalysisRequest(analysisId, "GLUC"), FIXED);

        fileRepo.saveAndFlush(file);
        em.clear();

        // No MultipleBagFetchException — findActiveById fetches the row only; bags via separate
        // calls.
        var fetched = fileRepo.findActiveById(file.getId()).orElseThrow();
        var samples = sampleRepo.findByLabFileId(fetched.getId());
        var analyses = requestRepo.findByLabFileId(fetched.getId());
        assertThat(samples).hasSize(1);
        assertThat(analyses).hasSize(1);
    }

    @Test
    void findActiveById_filters_soft_deleted() {
        var patientId = seedPatient();
        var file = freshFile(patientId);
        fileRepo.saveAndFlush(file);

        fileRepo.deleteById(file.getId());
        fileRepo.flush();

        // raw row still exists (soft delete) but findActiveById returns empty.
        assertThat(fileRepo.findById(file.getId())).isPresent();
        assertThat(fileRepo.findById(file.getId()).orElseThrow().getDeletedAt()).isNotNull();
        assertThat(fileRepo.findActiveById(file.getId())).isEmpty();
    }

    @Test
    void findActiveByPatientId_excludes_soft_deleted_and_orders_desc() {
        var patientId = seedPatient();
        var f1 = freshFile(patientId);
        var f2 = freshFile(patientId);
        var f3 = freshFile(patientId);
        fileRepo.saveAndFlush(f1);
        fileRepo.saveAndFlush(f2);
        fileRepo.saveAndFlush(f3);
        fileRepo.deleteById(f2.getId());
        fileRepo.flush();

        var active = fileRepo.findActiveByPatientId(patientId);
        assertThat(active)
                .extracting(LabFile::getId)
                .contains(f1.getId(), f3.getId())
                .doesNotContain(f2.getId());
    }

    @Test
    void existsByLabFileIdAndAnalysisId_round_trips_on_persisted_request() {
        var patientId = seedPatient();
        var disciplineId = seedDiscipline();
        var analysisId = seedAnalysis(disciplineId);

        var file = freshFile(patientId);
        file.addAnalysisRequest(new NewAnalysisRequest(analysisId, "GLUC"), FIXED);
        fileRepo.saveAndFlush(file);

        assertThat(requestRepo.existsByLabFileIdAndAnalysisId(file.getId(), analysisId)).isTrue();
        assertThat(requestRepo.existsByLabFileIdAndAnalysisId(file.getId(), Ulid.generate()))
                .isFalse();
    }

    @Test
    void search_filters_by_status_and_urgent() {
        var patientId = seedPatient();
        var openUrgent = seedLabFile(patientId, true, FIXED);
        var openNotUrgent = seedLabFile(patientId, false, FIXED);
        var awaitingUrgent = seedLabFile(patientId, true, FIXED);
        fileRepo.saveAndFlush(openUrgent);
        fileRepo.saveAndFlush(openNotUrgent);
        fileRepo.saveAndFlush(awaitingUrgent);

        // No public mutator promotes status — chunk 5 owns lifecycle. Bump status via SQL to
        // exercise the typed enum-equality branch of the JPQL search query.
        em.createNativeQuery("UPDATE lab_files SET status = 'AWAITING_RESULTS' WHERE id = ?")
                .setParameter(1, awaitingUrgent.getId())
                .executeUpdate();
        em.clear();

        var page =
                fileRepo.search(
                        null, null, true, LabFileStatus.OPEN, null, null, PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(LabFile::getId)
                .containsExactly(openUrgent.getId());
    }

    @Test
    void search_filters_by_patient_id() {
        var patientA = seedPatient();
        var patientB = seedPatient();
        var a1 = seedLabFile(patientA, false, FIXED);
        var a2 = seedLabFile(patientA, false, FIXED);
        var b1 = seedLabFile(patientB, false, FIXED);
        fileRepo.saveAndFlush(a1);
        fileRepo.saveAndFlush(a2);
        fileRepo.saveAndFlush(b1);

        var page = fileRepo.search(patientA, null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(LabFile::getId)
                .containsExactlyInAnyOrder(a1.getId(), a2.getId());
    }

    @Test
    void search_with_all_filters_null_returns_all_active() {
        var patientId = seedPatient();
        var active1 = seedLabFile(patientId, false, FIXED);
        var active2 = seedLabFile(patientId, false, FIXED);
        var deleted = seedLabFile(patientId, false, FIXED);
        fileRepo.saveAndFlush(active1);
        fileRepo.saveAndFlush(active2);
        fileRepo.saveAndFlush(deleted);
        fileRepo.deleteById(deleted.getId());
        fileRepo.flush();

        var page = fileRepo.search(null, null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(LabFile::getId)
                .containsExactlyInAnyOrder(active1.getId(), active2.getId());
    }

    @Test
    void search_filters_by_date_range() {
        var patientId = seedPatient();
        var now = FIXED.instant();
        var olderClock = Clock.fixed(now.minus(Duration.ofDays(2)), ZoneOffset.UTC);
        var insideClock = Clock.fixed(now.minus(Duration.ofHours(12)), ZoneOffset.UTC);
        var newerClock = Clock.fixed(now.plus(Duration.ofDays(2)), ZoneOffset.UTC);

        var older = seedLabFile(patientId, false, olderClock);
        var inside = seedLabFile(patientId, false, insideClock);
        var newer = seedLabFile(patientId, false, newerClock);
        fileRepo.saveAndFlush(older);
        fileRepo.saveAndFlush(inside);
        fileRepo.saveAndFlush(newer);

        var from = now.minus(Duration.ofDays(1));
        var to = now.plus(Duration.ofDays(1));

        var page = fileRepo.search(null, null, null, null, from, to, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(LabFile::getId).containsExactly(inside.getId());
    }
}
