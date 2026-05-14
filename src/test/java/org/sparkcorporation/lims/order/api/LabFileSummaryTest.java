package org.sparkcorporation.lims.order.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class LabFileSummaryTest {

    @Test
    void exposes_billing_block_with_discount_and_fees() {
        RecordComponent[] components = LabFileSummary.class.getRecordComponents();

        boolean hasBilling =
                Arrays.stream(components).anyMatch(rc -> rc.getName().equals("billing"));
        assertThat(hasBilling).isTrue();

        RecordComponent billing =
                Arrays.stream(components)
                        .filter(rc -> rc.getName().equals("billing"))
                        .findFirst()
                        .orElseThrow();
        assertThat(billing.getType()).isEqualTo(LabFileSummary.BillingBlock.class);

        RecordComponent[] bbFields = LabFileSummary.BillingBlock.class.getRecordComponents();
        assertThat(Arrays.stream(bbFields).map(RecordComponent::getName))
                .containsExactly(
                        "homeCollectionFee", "additionalFees", "discountKind", "discountValue");
    }
}
