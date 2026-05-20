package org.sparkcorporation.lims.cashbox.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.billing.api.InvoiceApi;
import org.sparkcorporation.lims.billing.api.InvoiceId;
import org.sparkcorporation.lims.billing.api.InvoiceNumber;
import org.sparkcorporation.lims.billing.api.InvoiceSummary;
import org.sparkcorporation.lims.cashbox.api.JournalEntryId;
import org.sparkcorporation.lims.cashbox.infra.JournalEntryRepository;
import org.sparkcorporation.lims.cashbox.infra.JournalEntryRowProjection;
import org.sparkcorporation.lims.iam.api.UserApi;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;

@ExtendWith(MockitoExtension.class)
class JournalServiceTest {

    @Mock private JournalEntryRepository journalRepo;
    @Mock private InvoiceApi invoiceApi;
    @Mock private LabFileApi labFileApi;
    @Mock private PatientApi patientApi;
    @Mock private UserApi userApi;

    private JournalService service;

    @BeforeEach
    void setUp() {
        service = new JournalService(journalRepo, invoiceApi, labFileApi, patientApi, userApi);
    }

    @Test
    void enrichesPaymentRowsWithInvoiceTotalAndRemaining() {
        var labWithInvoice = LabFileId.next();
        var labWithoutInvoice = LabFileId.next();
        var user = UserId.next();
        var register = CashRegisterId.generate();
        var rowWithInvoice =
                row(
                        "PAYMENT",
                        labWithInvoice.value(),
                        null,
                        "CASH",
                        new BigDecimal("3000"),
                        new BigDecimal("3000"),
                        user.value(),
                        register.value());
        var rowWithoutInvoice =
                row(
                        "PAYMENT",
                        labWithoutInvoice.value(),
                        null,
                        "CASH",
                        new BigDecimal("2000"),
                        new BigDecimal("2000"),
                        user.value(),
                        register.value());
        var page =
                new PageImpl<JournalEntryRowProjection>(
                        List.of(rowWithInvoice, rowWithoutInvoice), PageRequest.of(0, 20), 2);
        when(journalRepo.findJournalWithPaidSoFar(any(), any(), any(), any(), any()))
                .thenReturn(page);
        when(invoiceApi.findAllByLabFileIds(any()))
                .thenReturn(
                        Map.of(
                                labWithInvoice,
                                invoiceSummary(labWithInvoice, new BigDecimal("10000"))));
        when(labFileApi.findAllByIds(any())).thenReturn(Map.of());
        when(patientApi.findAllByIds(any())).thenReturn(Map.of());
        when(userApi.findAllByIds(any())).thenReturn(Map.of());

        var filters =
                new JournalFilters(
                        null,
                        Instant.parse("2026-05-18T00:00:00Z"),
                        Instant.parse("2026-05-19T00:00:00Z"),
                        null);

        var result = service.findJournal(filters, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
        var enriched = result.getContent().get(0);
        assertThat(enriched.invoiceTotal()).isEqualByComparingTo("10000");
        assertThat(enriched.paidSoFar()).isEqualByComparingTo("3000");
        assertThat(enriched.remaining()).isEqualByComparingTo("7000");
        var unenriched = result.getContent().get(1);
        assertThat(unenriched.invoiceTotal()).isNull();
        assertThat(unenriched.remaining()).isNull();
        assertThat(unenriched.paidSoFar()).isEqualByComparingTo("2000");
    }

    @Test
    void expenseAndHomeCollectionRowsHaveNullInvoiceTotalAndRemaining() {
        var lab = LabFileId.next();
        var user = UserId.next();
        var register = CashRegisterId.generate();
        var expenseRow =
                row(
                        "EXPENSE",
                        null,
                        null,
                        "CASH",
                        new BigDecimal("500"),
                        null,
                        user.value(),
                        register.value());
        var homeCollectionRow =
                row(
                        "HOME_COLLECTION_FEE",
                        lab.value(),
                        null,
                        null,
                        new BigDecimal("1500"),
                        null,
                        user.value(),
                        register.value());
        var page =
                new PageImpl<JournalEntryRowProjection>(
                        List.of(expenseRow, homeCollectionRow), PageRequest.of(0, 20), 2);
        when(journalRepo.findJournalWithPaidSoFar(any(), any(), any(), any(), any()))
                .thenReturn(page);
        when(invoiceApi.findAllByLabFileIds(any()))
                .thenReturn(Map.of(lab, invoiceSummary(lab, new BigDecimal("9999"))));
        when(labFileApi.findAllByIds(any())).thenReturn(Map.of());
        when(patientApi.findAllByIds(any())).thenReturn(Map.of());
        when(userApi.findAllByIds(any())).thenReturn(Map.of());

        var filters =
                new JournalFilters(
                        null,
                        Instant.parse("2026-05-18T00:00:00Z"),
                        Instant.parse("2026-05-19T00:00:00Z"),
                        null);

        var result = service.findJournal(filters, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
        for (var r : result.getContent()) {
            assertThat(r.invoiceTotal()).isNull();
            assertThat(r.paidSoFar()).isNull();
            assertThat(r.remaining()).isNull();
        }
    }

    @Test
    void emptyPageDoesNotCallEnrichmentApis() {
        when(journalRepo.findJournalWithPaidSoFar(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        when(invoiceApi.findAllByLabFileIds(Set.of())).thenReturn(Map.of());
        when(labFileApi.findAllByIds(Set.of())).thenReturn(Map.of());
        when(patientApi.findAllByIds(Set.of())).thenReturn(Map.of());
        when(userApi.findAllByIds(Set.of())).thenReturn(Map.of());

        var filters =
                new JournalFilters(
                        null,
                        Instant.parse("2026-05-18T00:00:00Z"),
                        Instant.parse("2026-05-19T00:00:00Z"),
                        null);

        var result = service.findJournal(filters, PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
    }

    private static JournalEntryRowProjection row(
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
                return "src-" + amount.toPlainString();
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

    private static InvoiceSummary invoiceSummary(LabFileId labFileId, BigDecimal totalAmount) {
        return new InvoiceSummary(
                InvoiceId.generate(),
                new InvoiceNumber("LTA26-0518-001"),
                labFileId,
                Instant.parse("2026-05-18T10:00:00Z"),
                org.sparkcorporation.lims.billing.api.InvoiceCustomerKind.PATIENT,
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
