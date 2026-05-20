package org.sparkcorporation.lims.cashbox.web;

import java.math.BigDecimal;
import java.util.Map;

import org.sparkcorporation.lims.billing.api.InvoiceSummary;
import org.sparkcorporation.lims.billing.api.PaymentMode;
import org.sparkcorporation.lims.cashbox.api.OperationKind;
import org.sparkcorporation.lims.cashbox.infra.JournalEntryRowProjection;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.iam.api.UserSummary;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;

final class JournalMapper {

    private JournalMapper() {}

    static JournalEntryRow toRow(
            JournalEntryRowProjection p,
            Map<LabFileId, LabFileSummary> labFiles,
            Map<PatientId, PatientSummary> patients,
            Map<UserId, UserSummary> users,
            Map<LabFileId, InvoiceSummary> invoices) {
        var operationKind = OperationKind.valueOf(p.getOperationKind());
        var paymentMode =
                p.getPaymentMode() == null ? null : PaymentMode.valueOf(p.getPaymentMode());

        var labFileId = p.getLabFileId();
        var patientId = p.getPatientId();
        var userId = p.getUserId();

        String labFileNumber = null;
        if (labFileId != null) {
            var summary = labFiles.get(new LabFileId(labFileId));
            if (summary != null) {
                labFileNumber = summary.registrationNumber().value();
            }
        }

        String patientLabel = null;
        if (patientId != null) {
            var summary = patients.get(new PatientId(patientId));
            if (summary != null) {
                patientLabel = composePatientLabel(summary);
            }
        }

        String userLabel = null;
        var userSummary = users.get(new UserId(userId));
        if (userSummary != null) {
            userLabel = userSummary.fullName();
        }

        BigDecimal invoiceTotal = null;
        BigDecimal remaining = null;
        if (operationKind == OperationKind.PAYMENT && labFileId != null) {
            var invoice = invoices.get(new LabFileId(labFileId));
            if (invoice != null) {
                invoiceTotal = invoice.totalAmount();
                if (p.getPaidSoFar() != null) {
                    remaining = invoiceTotal.subtract(p.getPaidSoFar());
                }
            }
        }

        BigDecimal paidSoFar = operationKind == OperationKind.PAYMENT ? p.getPaidSoFar() : null;

        return new JournalEntryRow(
                p.getOccurredAt(),
                operationKind,
                userId,
                userLabel,
                labFileId,
                labFileNumber,
                patientId,
                patientLabel,
                paymentMode,
                p.getAmount(),
                invoiceTotal,
                paidSoFar,
                remaining,
                p.getCashRegisterId());
    }

    private static String composePatientLabel(PatientSummary patient) {
        if (patient.firstNames() == null || patient.firstNames().isBlank()) {
            return patient.lastName();
        }
        return patient.lastName() + " " + patient.firstNames();
    }
}
