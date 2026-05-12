package org.sparkcorporation.lims.result.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.result.api.AntibiogramReadingId;
import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultLineId;

/**
 * Round-trip + null-handling for the three result-module {@code AttributeConverter}s. These are
 * pure boundary mappers between typed Id records and their {@code String} ULID DB representation;
 * the tests guard the null branches that JaCoCo otherwise flags as uncovered. Mirrors {@code
 * order::infra.OrderIdConvertersTest}.
 */
class ResultIdConvertersTest {

    private static final String SAMPLE_ULID = "01KQZ0WPVQA364NMRAJMKRQC84";

    @Test
    void resultIdConverter_round_trip_and_null() {
        var c = new ResultIdConverter();
        assertThat(c.convertToDatabaseColumn(null)).isNull();
        assertThat(c.convertToEntityAttribute(null)).isNull();
        assertThat(c.convertToDatabaseColumn(new ResultId(SAMPLE_ULID))).isEqualTo(SAMPLE_ULID);
        assertThat(c.convertToEntityAttribute(SAMPLE_ULID)).isEqualTo(new ResultId(SAMPLE_ULID));
    }

    @Test
    void resultLineIdConverter_round_trip_and_null() {
        var c = new ResultLineIdConverter();
        assertThat(c.convertToDatabaseColumn(null)).isNull();
        assertThat(c.convertToEntityAttribute(null)).isNull();
        assertThat(c.convertToDatabaseColumn(new ResultLineId(SAMPLE_ULID))).isEqualTo(SAMPLE_ULID);
        assertThat(c.convertToEntityAttribute(SAMPLE_ULID))
                .isEqualTo(new ResultLineId(SAMPLE_ULID));
    }

    @Test
    void antibiogramReadingIdConverter_round_trip_and_null() {
        var c = new AntibiogramReadingIdConverter();
        assertThat(c.convertToDatabaseColumn(null)).isNull();
        assertThat(c.convertToEntityAttribute(null)).isNull();
        assertThat(c.convertToDatabaseColumn(new AntibiogramReadingId(SAMPLE_ULID)))
                .isEqualTo(SAMPLE_ULID);
        assertThat(c.convertToEntityAttribute(SAMPLE_ULID))
                .isEqualTo(new AntibiogramReadingId(SAMPLE_ULID));
    }
}
