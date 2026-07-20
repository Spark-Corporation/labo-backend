package org.sparkcorporation.lims.cashbox.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.billing.api.InvoiceCustomerKind;
import org.sparkcorporation.lims.billing.api.InvoiceId;
import org.sparkcorporation.lims.billing.api.InvoiceNumber;
import org.sparkcorporation.lims.billing.api.InvoiceSummary;
import org.sparkcorporation.lims.cashbox.api.JournalEntryId;
import org.sparkcorporation.lims.cashbox.api.OperationKind;
import org.sparkcorporation.lims.cashbox.infra.JournalEntryRowProjection;
import org.sparkcorporation.lims.iam.api.Role;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.iam.api.UserSummary;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.order.api.LabFileSummary.BillingBlock;
import org.sparkcorporation.lims.order.api.LabFileSummary.PatientSnapshot;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

class JournalMapperTest {

    @Test
    void paymentRowRemainingIsInvoiceTotalMinusPaidSoFar() {
        var labFile = LabFileId.next();
        var patient = PatientId.next();
        var user = UserId.next();
        var register = CashRegisterId.generate();
        var projection =
                projection(
                        "PAYMENT",
                        labFile.value(),
                        patient.value(),
                        "CASH",
                        new BigDecimal("3000"),
                        new BigDecimal("7000"),
                        user.value(),
                        register.value());

        var row =
                JournalMapper.toRow(
                        projection,
                        Map.of(labFile, labFileSummary(labFile, patient, "LF-260518-9001")),
                        Map.of(patient, patientSummary(patient, "DOE", "Jane")),
                        Map.of(user, userSummary(user, "Dr. Bob")),
                        Map.of(labFile, invoiceSummary(labFile, new BigDecimal("10000"))));

        assertThat(row.operationKind()).isEqualTo(OperationKind.PAYMENT);
        assertThat(row.labFileNumber()).isEqualTo("LF-260518-9001");
        assertThat(row.patientLabel()).isEqualTo("DOE Jane");
        assertThat(row.userLabel()).isEqualTo("Dr. Bob");
        assertThat(row.invoiceTotal()).isEqualByComparingTo("10000");
        assertThat(row.paidSoFar()).isEqualByComparingTo("7000");
        assertThat(row.remaining()).isEqualByComparingTo("3000");
    }

    @Test
    void expenseRowHasNullInvoiceTotalPaidSoFarAndRemaining() {
        var user = UserId.next();
        var register = CashRegisterId.generate();
        var projection =
                projection(
                        "EXPENSE",
                        null,
                        null,
                        "CASH",
                        new BigDecimal("500"),
                        null,
                        user.value(),
                        register.value());

        var row =
                JournalMapper.toRow(
                        projection,
                        Map.of(),
                        Map.of(),
                        Map.of(user, userSummary(user, "Cashier")),
                        Map.of());

        assertThat(row.operationKind()).isEqualTo(OperationKind.EXPENSE);
        assertThat(row.invoiceTotal()).isNull();
        assertThat(row.paidSoFar()).isNull();
        assertThat(row.remaining()).isNull();
        assertThat(row.labFileNumber()).isNull();
    }

    @Test
    void paymentRowWithoutInvoiceLeavesInvoiceTotalAndRemainingNull() {
        var labFile = LabFileId.next();
        var user = UserId.next();
        var register = CashRegisterId.generate();
        var projection =
                projection(
                        "PAYMENT",
                        labFile.value(),
                        null,
                        "CASH",
                        new BigDecimal("3000"),
                        new BigDecimal("3000"),
                        user.value(),
                        register.value());

        var row = JournalMapper.toRow(projection, Map.of(), Map.of(), Map.of(), Map.of());

        assertThat(row.invoiceTotal()).isNull();
        assertThat(row.remaining()).isNull();
        assertThat(row.paidSoFar()).isEqualByComparingTo("3000");
    }

    @Test
    void patientLabelOmitsFirstNamesWhenBlank() {
        var patient = PatientId.next();
        var user = UserId.next();
        var register = CashRegisterId.generate();
        var projection =
                projection(
                        "EXPENSE",
                        null,
                        patient.value(),
                        "CASH",
                        new BigDecimal("100"),
                        null,
                        user.value(),
                        register.value());

        var row =
                JournalMapper.toRow(
                        projection,
                        Map.of(),
                        Map.of(patient, patientSummary(patient, "SMITH", null)),
                        Map.of(),
                        Map.of());

        assertThat(row.patientLabel()).isEqualTo("SMITH");
    }

    private static JournalEntryRowProjection projection(
            String operationKind,
            @Nullable String labFileId,
            @Nullable String patientId,
            @Nullable String paymentMode,
            BigDecimal amount,
            @Nullable BigDecimal paidSoFar,
            String userId,
            String cashRegisterId) {
        return new JournalEntryRowProjection() {
            @Override
            public String getId() {
                return JournalEntryId.generate().value();
            }

            @Override
            public String getOperationKind() {
                return operationKind;
            }

            @Override
            public String getSourceId() {
                return "src";
            }

            @Override
            public String getCashRegisterId() {
                return cashRegisterId;
            }

            @Override
            public String getUserId() {
                return userId;
            }

            @Override
            public String getLabFileId() {
                return labFileId;
            }

            @Override
            public String getPatientId() {
                return patientId;
            }

            @Override
            public String getPaymentMode() {
                return paymentMode;
            }

            @Override
            public BigDecimal getAmount() {
                return amount;
            }

            @Override
            public Instant getOccurredAt() {
                return Instant.parse("2026-05-18T10:00:00Z");
            }

            @Override
            public BigDecimal getPaidSoFar() {
                return paidSoFar;
            }
        };
    }

    private static LabFileSummary labFileSummary(LabFileId id, PatientId patient, String regNum) {
        return new LabFileSummary(
                id,
                new LabFileRegistrationNumber(regNum),
                new PatientSnapshot(patient.value(), "LAB-1", "DOE", "Jane", Sex.F),
                null,
                null,
                false,
                LabFileStatus.OPEN,
                List.of(),
                List.of(),
                null,
                null,
                new BillingBlock(BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                Instant.parse("2026-05-18T10:00:00Z"),
                null,
                null,
                null,
                null);
    }

    private static PatientSummary patientSummary(
            PatientId id, String lastName, @Nullable String firstNames) {
        return new PatientSummary(
                id,
                "LAB-001",
                lastName,
                firstNames,
                Civility.MADAME,
                Sex.F,
                null,
                null,
                null,
                false,
                false);
    }

    private static UserSummary userSummary(UserId id, String fullName) {
        return new UserSummary(
                id, fullName.toLowerCase() + "@test", fullName, true, Set.of(Role.CASHIER));
    }

    private static InvoiceSummary invoiceSummary(LabFileId labFileId, BigDecimal totalAmount) {
        return new InvoiceSummary(
                InvoiceId.generate(),
                new InvoiceNumber("LTA26-0518-001"),
                labFileId,
                Instant.parse("2026-05-18T10:00:00Z"),
                InvoiceCustomerKind.PATIENT,
                PatientId.next().value(),
                "DOE",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                totalAmount,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                totalAmount,
                List.of());
    }
}
