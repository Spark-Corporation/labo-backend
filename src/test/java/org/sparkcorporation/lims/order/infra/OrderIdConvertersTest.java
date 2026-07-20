package org.sparkcorporation.lims.order.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.QuoteId;
import org.sparkcorporation.lims.order.api.QuoteLineId;
import org.sparkcorporation.lims.order.api.SampleId;

/**
 * Round-trip + null-handling for the 5 order-module {@code AttributeConverter}s. These are pure
 * boundary mappers between typed Id records and their {@code String} ULID DB representation; the
 * tests guard the null branches that JaCoCo otherwise flags as uncovered.
 */
class OrderIdConvertersTest {

    private static final String SAMPLE_ULID = "01KQZ0WPVQA364NMRAJMKRQC84";

    @Test
    void labFileIdConverter_round_trip_and_null() {
        var c = new LabFileIdConverter();
        assertThat(c.convertToDatabaseColumn(null)).isNull();
        assertThat(c.convertToEntityAttribute(null)).isNull();
        assertThat(c.convertToDatabaseColumn(new LabFileId(SAMPLE_ULID))).isEqualTo(SAMPLE_ULID);
        assertThat(c.convertToEntityAttribute(SAMPLE_ULID)).isEqualTo(new LabFileId(SAMPLE_ULID));
    }

    @Test
    void sampleIdConverter_round_trip_and_null() {
        var c = new SampleIdConverter();
        assertThat(c.convertToDatabaseColumn(null)).isNull();
        assertThat(c.convertToEntityAttribute(null)).isNull();
        assertThat(c.convertToDatabaseColumn(new SampleId(SAMPLE_ULID))).isEqualTo(SAMPLE_ULID);
        assertThat(c.convertToEntityAttribute(SAMPLE_ULID)).isEqualTo(new SampleId(SAMPLE_ULID));
    }

    @Test
    void analysisRequestIdConverter_round_trip_and_null() {
        var c = new AnalysisRequestIdConverter();
        assertThat(c.convertToDatabaseColumn(null)).isNull();
        assertThat(c.convertToEntityAttribute(null)).isNull();
        assertThat(c.convertToDatabaseColumn(new AnalysisRequestId(SAMPLE_ULID)))
                .isEqualTo(SAMPLE_ULID);
        assertThat(c.convertToEntityAttribute(SAMPLE_ULID))
                .isEqualTo(new AnalysisRequestId(SAMPLE_ULID));
    }

    @Test
    void quoteIdConverter_round_trip_and_null() {
        var c = new QuoteIdConverter();
        assertThat(c.convertToDatabaseColumn(null)).isNull();
        assertThat(c.convertToEntityAttribute(null)).isNull();
        assertThat(c.convertToDatabaseColumn(new QuoteId(SAMPLE_ULID))).isEqualTo(SAMPLE_ULID);
        assertThat(c.convertToEntityAttribute(SAMPLE_ULID)).isEqualTo(new QuoteId(SAMPLE_ULID));
    }

    @Test
    void quoteLineIdConverter_round_trip_and_null() {
        var c = new QuoteLineIdConverter();
        assertThat(c.convertToDatabaseColumn(null)).isNull();
        assertThat(c.convertToEntityAttribute(null)).isNull();
        assertThat(c.convertToDatabaseColumn(new QuoteLineId(SAMPLE_ULID))).isEqualTo(SAMPLE_ULID);
        assertThat(c.convertToEntityAttribute(SAMPLE_ULID)).isEqualTo(new QuoteLineId(SAMPLE_ULID));
    }
}
