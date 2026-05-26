package org.sparkcorporation.lims.billing.infra.pdf;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.billing.api.CashRegisterApi;
import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.billing.api.PaymentId;
import org.sparkcorporation.lims.billing.api.PaymentMode;
import org.sparkcorporation.lims.billing.infra.PaymentRepository;
import org.sparkcorporation.lims.iam.api.UserApi;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.labsettings.api.LabSettingsApi;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;

/**
 * Builds a {@link PaymentReceiptModel} from a payment id. Reads via {@code PaymentRepository}
 * directly (same module) plus cross-module {@code *Api} snapshots. Stateless, transactional
 * read-only.
 *
 * <p>Throws {@link PaymentInvalidIdException} when the id is unknown — mapped to 404 by {@code
 * billing/web/BillingProblemDetailAdvice}.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentReceiptModelAssembler {

    private static final DateTimeFormatter FRENCH_DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH)
                    .withZone(ZoneId.of("UTC"));

    private final PaymentRepository paymentRepo;
    private final LabFileApi labFileApi;
    private final PatientApi patientApi;
    private final CashRegisterApi cashRegisterApi;
    private final UserApi userApi;
    private final LabSettingsApi labSettingsApi;

    public PaymentReceiptModel assemble(PaymentId paymentId) {
        var payment =
                paymentRepo
                        .findById(paymentId.value())
                        .orElseThrow(() -> new PaymentInvalidIdException(paymentId));

        var labFile = labFileApi.requireById(new LabFileId(payment.getLabFileId()));
        var patient = patientApi.findById(new PatientId(labFile.patient().id())).orElseThrow();
        var register =
                cashRegisterApi
                        .findById(new CashRegisterId(payment.getCashRegisterId()))
                        .orElseThrow();
        var cashier = userApi.findById(new UserId(payment.getRecordedByUserId())).orElseThrow();
        var lab = labSettingsApi.get();

        return new PaymentReceiptModel(
                new PaymentReceiptModel.ReceiptHeader(
                        "Reçu N° " + payment.getId(),
                        FRENCH_DATE_TIME.format(payment.getRecordedAt()),
                        payment.getRecordedAt()),
                new PaymentReceiptModel.LabFileSummary(
                        labFile.id().value(), labFile.registrationNumber().value()),
                new PaymentReceiptModel.PatientSummary(
                        patient.lastName(),
                        patient.firstNames() == null ? "" : patient.firstNames(),
                        patient.sex().name()),
                new PaymentReceiptModel.PaymentSummary(
                        modeDisplay(payment.getMode()), amountDisplay(payment.getAmount())),
                new PaymentReceiptModel.CashRegisterSummary(register.code(), register.label()),
                new PaymentReceiptModel.UserSummary(cashier.fullName()),
                new PaymentReceiptModel.LabIdentity(
                        lab.name(), lab.address(),
                        lab.accreditationNumber(), lab.managerName()),
                Locale.FRENCH);
    }

    static String modeDisplay(PaymentMode mode) {
        return switch (mode) {
            case CASH -> "Espèces";
            case CHEQUE -> "Chèque";
            case CARD_TPE -> "Carte TPE";
            case FLOOZ -> "Flooz (Moov Africa)";
            case MIXX_BY_YAS -> "Mixx by Yas";
        };
    }

    static String amountDisplay(BigDecimal amount) {
        var fr = NumberFormat.getInstance(Locale.FRENCH);
        fr.setGroupingUsed(true);
        fr.setMinimumFractionDigits(0);
        fr.setMaximumFractionDigits(0);
        return fr.format(amount) + " F CFA";
    }
}
