package org.sparkcorporation.lims.billing.domain;

import java.util.Objects;

import org.sparkcorporation.lims.billing.api.InvoiceCustomerKind;

/**
 * Sealed customer reference captured at invoice issuance. The {@link #nameSnapshot()} is frozen on
 * the invoice and never reflects subsequent edits to the underlying patient / insurance / partner
 * row — see spec §5.1 invariant 5 ("customer_name_snapshot is captured at issuance").
 */
public sealed interface Customer
        permits Customer.PatientCustomer, Customer.InsuranceCustomer, Customer.PartnerCustomer {

    String id();

    String nameSnapshot();

    InvoiceCustomerKind kind();

    record PatientCustomer(String id, String nameSnapshot) implements Customer {
        public PatientCustomer {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(nameSnapshot, "nameSnapshot");
        }

        @Override
        public InvoiceCustomerKind kind() {
            return InvoiceCustomerKind.PATIENT;
        }
    }

    record InsuranceCustomer(String id, String nameSnapshot) implements Customer {
        public InsuranceCustomer {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(nameSnapshot, "nameSnapshot");
        }

        @Override
        public InvoiceCustomerKind kind() {
            return InvoiceCustomerKind.INSURANCE;
        }
    }

    record PartnerCustomer(String id, String nameSnapshot) implements Customer {
        public PartnerCustomer {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(nameSnapshot, "nameSnapshot");
        }

        @Override
        public InvoiceCustomerKind kind() {
            return InvoiceCustomerKind.PARTNER;
        }
    }
}
