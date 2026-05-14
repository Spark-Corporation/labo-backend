package org.sparkcorporation.lims.billing.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.sparkcorporation.lims.billing.api.InvoiceCustomerKind;
import org.sparkcorporation.lims.billing.api.InvoiceId;
import org.sparkcorporation.lims.billing.api.InvoiceNumber;
import org.sparkcorporation.lims.billing.domain.BillingSnapshot;
import org.sparkcorporation.lims.billing.domain.Customer.PatientCustomer;
import org.sparkcorporation.lims.billing.domain.Invoice;
import org.sparkcorporation.lims.billing.domain.PricingResult;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.order.api.LabFileSummary.BillingBlock;
import org.sparkcorporation.lims.order.api.LabFileSummary.PatientSnapshot;
import org.sparkcorporation.lims.shared.domain.Sex;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Unit tests for {@link DefaultInvoiceApi}. Exercises the read projection + the patient-resolution
 * branches in {@code search(...)} so coverage of the thin mapping layer goes from 0 to ~100 %.
 */
class DefaultInvoiceApiTest {

    private static final Instant ISSUED_AT = Instant.parse("2026-05-13T10:00:00Z");

    private final InvoiceRepository repo = mock(InvoiceRepository.class);
    private final LabFileApi labFileApi = mock(LabFileApi.class);

    private final DefaultInvoiceApi api = new DefaultInvoiceApi(repo, labFileApi);

    private Invoice freshInvoice() {
        var labFile = new LabFileId(Ulid.generate());
        var pricing =
                new PricingResult(
                        new BigDecimal("75000"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("75000"),
                        List.of(
                                new PricingResult.LineResult(
                                        1, new BigDecimal("1500"), new BigDecimal("75000"))));
        var billing = new BillingSnapshot(BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        var customer = new PatientCustomer(Ulid.generate(), "DOE Jane");
        return Invoice.issue(
                new InvoiceNumber("LAB26-0513-001"),
                labFile,
                customer,
                billing,
                pricing,
                ISSUED_AT);
    }

    private LabFileSummary labFileSummary(String labFileUlid, String patientUlid) {
        return new LabFileSummary(
                new LabFileId(labFileUlid),
                new LabFileRegistrationNumber("LF-260513-0001"),
                new PatientSnapshot(patientUlid, "LAB-260513-001", "DOE", "Jane", Sex.F),
                null,
                null,
                false,
                LabFileStatus.OPEN,
                List.of(),
                List.of(),
                null,
                null,
                new BillingBlock(BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                ISSUED_AT,
                null);
    }

    // --- findById ----------------------------------------------------------------------------

    @Test
    void findById_returns_summary_when_present() {
        var invoice = freshInvoice();
        when(repo.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        var found = api.findById(InvoiceId.of(invoice.getId()));

        assertThat(found).isPresent();
        assertThat(found.get().id().value()).isEqualTo(invoice.getId());
        assertThat(found.get().number().value()).isEqualTo("LAB26-0513-001");
        assertThat(found.get().customerKind()).isEqualTo(InvoiceCustomerKind.PATIENT);
        assertThat(found.get().netAmount()).isEqualByComparingTo("75000");
        assertThat(found.get().lines()).hasSize(0); // populateLineSnapshots not called
    }

    @Test
    void findById_returns_empty_when_absent() {
        when(repo.findById(anyString())).thenReturn(Optional.empty());

        var found = api.findById(InvoiceId.generate());

        assertThat(found).isEmpty();
    }

    // --- findByLabFileId ---------------------------------------------------------------------

    @Test
    void findByLabFileId_returns_summary_when_present() {
        var invoice = freshInvoice();
        when(repo.findByLabFileId(invoice.getLabFileId())).thenReturn(Optional.of(invoice));

        var found = api.findByLabFileId(new LabFileId(invoice.getLabFileId()));

        assertThat(found).isPresent();
        assertThat(found.get().labFileId().value()).isEqualTo(invoice.getLabFileId());
    }

    @Test
    void findByLabFileId_returns_empty_when_absent() {
        when(repo.findByLabFileId(anyString())).thenReturn(Optional.empty());

        var found = api.findByLabFileId(new LabFileId(Ulid.generate()));

        assertThat(found).isEmpty();
    }

    // --- findByNumber ------------------------------------------------------------------------

    @Test
    void findByNumber_returns_summary_when_present() {
        var invoice = freshInvoice();
        when(repo.findByNumber(invoice.getNumber())).thenReturn(Optional.of(invoice));

        var found = api.findByNumber(new InvoiceNumber(invoice.getNumber()));

        assertThat(found).isPresent();
        assertThat(found.get().number().value()).isEqualTo(invoice.getNumber());
    }

    @Test
    void findByNumber_returns_empty_when_absent() {
        when(repo.findByNumber(anyString())).thenReturn(Optional.empty());

        var found = api.findByNumber(new InvoiceNumber("XYZ26-0513-999"));

        assertThat(found).isEmpty();
    }

    // --- search ------------------------------------------------------------------------------

    @Test
    void search_with_all_filters_null_passes_null_labFileIds_to_repo() {
        Pageable page = PageRequest.of(0, 20);
        when(repo.search(isNull(), isNull(), isNull(), isNull(), isNull(), eq(page)))
                .thenReturn(new PageImpl<>(List.of(), page, 0));

        Page<?> result = api.search(null, null, null, null, null, null, page);

        assertThat(result.getContent()).isEmpty();
        // labFileApi never consulted when patientId is null
        verify(labFileApi, never()).findActiveByPatient(anyString());
    }

    @Test
    void search_with_only_labFileId_wraps_in_singleton_list() {
        Pageable page = PageRequest.of(0, 20);
        var labFileUlid = Ulid.generate();
        var invoice = freshInvoice();
        when(repo.search(any(), any(), any(), any(), eq(List.of(labFileUlid)), eq(page)))
                .thenReturn(new PageImpl<>(List.of(invoice), page, 1));

        var result = api.search(null, null, null, null, new LabFileId(labFileUlid), null, page);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(labFileApi, never()).findActiveByPatient(anyString());
    }

    @Test
    void search_with_patientId_resolves_lab_files_via_LabFileApi() {
        Pageable page = PageRequest.of(0, 20);
        var patientUlid = Ulid.generate();
        var labFileUlid = Ulid.generate();
        when(labFileApi.findActiveByPatient(patientUlid))
                .thenReturn(List.of(labFileSummary(labFileUlid, patientUlid)));
        var invoice = freshInvoice();
        when(repo.search(any(), any(), any(), any(), eq(List.of(labFileUlid)), eq(page)))
                .thenReturn(new PageImpl<>(List.of(invoice), page, 1));

        var result = api.search(null, null, null, null, null, patientUlid, page);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(labFileApi).findActiveByPatient(patientUlid);
    }

    @Test
    void search_with_patientId_and_matching_labFileId_keeps_singleton() {
        Pageable page = PageRequest.of(0, 20);
        var patientUlid = Ulid.generate();
        var labFileUlid = Ulid.generate();
        when(labFileApi.findActiveByPatient(patientUlid))
                .thenReturn(List.of(labFileSummary(labFileUlid, patientUlid)));
        var invoice = freshInvoice();
        when(repo.search(any(), any(), any(), any(), eq(List.of(labFileUlid)), eq(page)))
                .thenReturn(new PageImpl<>(List.of(invoice), page, 1));

        var result =
                api.search(null, null, null, null, new LabFileId(labFileUlid), patientUlid, page);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(labFileApi).findActiveByPatient(patientUlid);
    }

    @Test
    void search_with_patientId_and_nonmatching_labFileId_returns_empty_short_circuit() {
        Pageable page = PageRequest.of(0, 20);
        var patientUlid = Ulid.generate();
        var resolvedLabFile = Ulid.generate();
        var requestedLabFile = Ulid.generate();
        when(labFileApi.findActiveByPatient(patientUlid))
                .thenReturn(List.of(labFileSummary(resolvedLabFile, patientUlid)));

        var result =
                api.search(
                        null, null, null, null, new LabFileId(requestedLabFile), patientUlid, page);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        // Repo never consulted — short-circuit.
        verify(repo, never()).search(any(), any(), any(), anyString(), any(), any(Pageable.class));
    }

    @Test
    void search_with_patientId_and_no_resolved_files_returns_empty() {
        Pageable page = PageRequest.of(0, 20);
        var patientUlid = Ulid.generate();
        when(labFileApi.findActiveByPatient(patientUlid)).thenReturn(List.of());

        var result = api.search(null, null, null, null, null, patientUlid, page);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(repo, never()).search(any(), any(), any(), anyString(), any(), any(Pageable.class));
    }

    @Test
    void search_passes_through_customer_filters_to_repo() {
        Pageable page = PageRequest.of(0, 20);
        var from = Instant.parse("2026-05-01T00:00:00Z");
        var to = Instant.parse("2026-06-01T00:00:00Z");
        var customerUlid = Ulid.generate();
        when(repo.search(
                        eq(from),
                        eq(to),
                        eq(InvoiceCustomerKind.PATIENT),
                        eq(customerUlid),
                        isNull(),
                        eq(page)))
                .thenReturn(new PageImpl<>(List.of(), page, 0));

        api.search(from, to, InvoiceCustomerKind.PATIENT, customerUlid, null, null, page);

        verify(repo)
                .search(
                        eq(from),
                        eq(to),
                        eq(InvoiceCustomerKind.PATIENT),
                        eq(customerUlid),
                        isNull(),
                        eq(page));
    }
}
