package org.sparkcorporation.lims.billing.infra.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.sparkcorporation.lims.billing.api.CashRegisterApi;
import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.billing.api.CashRegisterSummary;
import org.sparkcorporation.lims.billing.api.PaymentId;
import org.sparkcorporation.lims.billing.api.PaymentMode;
import org.sparkcorporation.lims.billing.domain.Payment;
import org.sparkcorporation.lims.billing.infra.PaymentRepository;
import org.sparkcorporation.lims.iam.api.Role;
import org.sparkcorporation.lims.iam.api.UserApi;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.iam.api.UserSummary;
import org.sparkcorporation.lims.labsettings.api.LabSettings;
import org.sparkcorporation.lims.labsettings.api.LabSettingsApi;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentReceiptModelAssemblerTest {

    // Valid 26-char Crockford Base32 ULIDs (alphabet excludes I, L, O, U).
    private static final String PAYMENT_ULID = "01HZX00000000000000000PAYM";
    private static final String LAB_FILE_ULID = "01HZX0000000000000000KBFE0";
    private static final String PATIENT_ULID = "01HZX0000000000000000PATE0";
    private static final String CASHIER_ULID = "01HZX000000000000000CSHR00";
    private static final String REGISTER_ULID = "01HZX000000000000000RGSTR0";

    @Mock private PaymentRepository paymentRepo;
    @Mock private LabFileApi labFileApi;
    @Mock private PatientApi patientApi;
    @Mock private CashRegisterApi cashRegisterApi;
    @Mock private UserApi userApi;
    @Mock private LabSettingsApi labSettingsApi;

    @InjectMocks private PaymentReceiptModelAssembler assembler;

    @Test
    void assemble_returns_populated_model_for_happy_path() {
        var paymentId = new PaymentId(PAYMENT_ULID);
        var recordedAt = Instant.parse("2026-05-22T10:30:00Z");
        var payment = mockPayment(paymentId, recordedAt);
        when(paymentRepo.findById(PAYMENT_ULID)).thenReturn(Optional.of(payment));

        when(labFileApi.requireById(new LabFileId(LAB_FILE_ULID))).thenReturn(labFileSummary());
        when(patientApi.findById(new PatientId(PATIENT_ULID)))
                .thenReturn(Optional.of(patientSummary()));
        when(cashRegisterApi.findById(new CashRegisterId(REGISTER_ULID)))
                .thenReturn(
                        Optional.of(
                                new CashRegisterSummary(
                                        new CashRegisterId(REGISTER_ULID),
                                        "CR-01",
                                        "Caisse principale",
                                        1,
                                        Set.of(CASHIER_ULID),
                                        false,
                                        Instant.parse("2026-05-01T00:00:00Z"))));
        when(userApi.findById(new UserId(CASHIER_ULID)))
                .thenReturn(
                        Optional.of(
                                new UserSummary(
                                        new UserId(CASHIER_ULID),
                                        "cashier@test.local",
                                        "Dr. KOFFI Adjoa",
                                        true,
                                        Set.<Role>of())));
        when(labSettingsApi.get())
                .thenReturn(
                        new LabSettings(
                                "Lab Sparkcorp",
                                "12 Avenue du Test",
                                "ACC-2026-001",
                                "Dr. Manager",
                                Instant.parse("2026-05-22T00:00:00Z")));

        var model = assembler.assemble(paymentId);

        assertThat(model.header().receiptNumber()).isEqualTo("Reçu N° " + PAYMENT_ULID);
        assertThat(model.header().issuedAtDisplay()).isEqualTo("22/05/2026 10:30");
        assertThat(model.header().issuedAtUtc()).isEqualTo(recordedAt);
        assertThat(model.labFile().labFileId()).isEqualTo(LAB_FILE_ULID);
        assertThat(model.labFile().dailyId()).isEqualTo("LF-260522-0001");
        assertThat(model.patient().lastName()).isEqualTo("DOE");
        assertThat(model.patient().firstName()).isEqualTo("Jane");
        assertThat(model.patient().sex()).isEqualTo("F");
        assertThat(model.payment().modeDisplay()).isEqualTo("Espèces");
        assertThat(model.payment().amountDisplay())
                .isEqualTo(
                        NumberFormat.getInstance(Locale.FRENCH).format(new BigDecimal("5000"))
                                + " F CFA");
        assertThat(model.cashRegister().code()).isEqualTo("CR-01");
        assertThat(model.cashRegister().label()).isEqualTo("Caisse principale");
        assertThat(model.cashier().displayName()).isEqualTo("Dr. KOFFI Adjoa");
        assertThat(model.labIdentity().name()).isEqualTo("Lab Sparkcorp");
        assertThat(model.labIdentity().address()).isEqualTo("12 Avenue du Test");
        assertThat(model.labIdentity().accreditationNumber()).isEqualTo("ACC-2026-001");
        assertThat(model.labIdentity().managerName()).isEqualTo("Dr. Manager");
        assertThat(model.locale()).isEqualTo(Locale.FRENCH);
    }

    @Test
    void assemble_throws_PaymentInvalidIdException_when_unknown() {
        var paymentId = new PaymentId(PAYMENT_ULID);
        when(paymentRepo.findById(PAYMENT_ULID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assembler.assemble(paymentId))
                .isInstanceOf(PaymentInvalidIdException.class)
                .extracting(ex -> ((PaymentInvalidIdException) ex).paymentId())
                .isEqualTo(paymentId);
    }

    @ParameterizedTest
    @CsvSource({
        "CASH, Espèces",
        "CHEQUE, Chèque",
        "CARD_TPE, Carte TPE",
        "FLOOZ, Flooz (Moov Africa)",
        "MIXX_BY_YAS, Mixx by Yas"
    })
    void modeDisplay_renders_all_PaymentMode_values(PaymentMode mode, String expected) {
        assertThat(PaymentReceiptModelAssembler.modeDisplay(mode)).isEqualTo(expected);
    }

    @Test
    void amountDisplay_uses_French_locale_grouping_separator() {
        var rendered = PaymentReceiptModelAssembler.amountDisplay(new BigDecimal("12345"));

        // Don't hardcode U+0020 — JDK 17+ uses U+202F (narrow no-break space) for Locale.FRENCH.
        // Assert against NumberFormat directly to survive JDK separator drift.
        var expected =
                NumberFormat.getInstance(Locale.FRENCH).format(new BigDecimal("12345")) + " F CFA";
        assertThat(rendered).isEqualTo(expected);
    }

    // ----- helpers -----

    private static Payment mockPayment(PaymentId paymentId, Instant recordedAt) {
        var p = org.mockito.Mockito.mock(Payment.class);
        when(p.getId()).thenReturn(paymentId.value());
        when(p.getLabFileId()).thenReturn(LAB_FILE_ULID);
        when(p.getCashRegisterId()).thenReturn(REGISTER_ULID);
        when(p.getRecordedByUserId()).thenReturn(CASHIER_ULID);
        when(p.getMode()).thenReturn(PaymentMode.CASH);
        when(p.getAmount()).thenReturn(new BigDecimal("5000"));
        when(p.getRecordedAt()).thenReturn(recordedAt);
        return p;
    }

    private static LabFileSummary labFileSummary() {
        return new LabFileSummary(
                new LabFileId(LAB_FILE_ULID),
                new LabFileRegistrationNumber("LF-260522-0001"),
                new LabFileSummary.PatientSnapshot(PATIENT_ULID, "LAB-00001", "DOE", "Jane", Sex.F),
                null,
                null,
                false,
                LabFileStatus.OPEN,
                List.of(),
                List.of(),
                null,
                null,
                new LabFileSummary.BillingBlock(BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                Instant.parse("2026-05-22T08:00:00Z"),
                null);
    }

    private static PatientSummary patientSummary() {
        return new PatientSummary(
                new PatientId(PATIENT_ULID),
                "LAB-00001",
                "DOE",
                "Jane",
                Civility.MADAME,
                Sex.F,
                LocalDate.of(1985, 1, 1),
                null,
                null,
                false,
                false);
    }
}
