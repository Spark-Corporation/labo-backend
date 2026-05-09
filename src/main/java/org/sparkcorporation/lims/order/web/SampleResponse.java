package org.sparkcorporation.lims.order.web;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jspecify.annotations.Nullable;

/**
 * Web-tier sealed mirror of {@link
 * org.sparkcorporation.lims.order.api.LabFileSummary.SampleSummary}. The discriminator surfaces in
 * JSON as {@code type} so clients can branch on sub-type.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SampleResponse.InLab.class, name = "IN_LAB"),
    @JsonSubTypes.Type(value = SampleResponse.Home.class, name = "HOME"),
    @JsonSubTypes.Type(value = SampleResponse.ExternalReceived.class, name = "EXTERNAL_RECEIVED")
})
public sealed interface SampleResponse
        permits SampleResponse.InLab, SampleResponse.Home, SampleResponse.ExternalReceived {

    String id();

    String dailyId();

    String analysisCode();

    String recipientCode();

    @Nullable Instant collectedAt();

    @Nullable String collectedByUserId();

    /** In-lab counter sample — no extra fields. */
    record InLab(
            String id,
            String dailyId,
            String analysisCode,
            String recipientCode,
            Instant collectedAt,
            @Nullable String collectedByUserId)
            implements SampleResponse {}

    /** Home-collected sample with address / window / préleveur. */
    record Home(
            String id,
            String dailyId,
            String analysisCode,
            String recipientCode,
            Instant collectedAt,
            @Nullable String collectedByUserId,
            String addressLine,
            int distanceKm,
            Instant windowStart,
            Instant windowEnd,
            String preleveurCode)
            implements SampleResponse {}

    /** Externally-received sample with origin / reception metadata. */
    record ExternalReceived(
            String id,
            String dailyId,
            String analysisCode,
            String recipientCode,
            @Nullable Instant collectedAt,
            @Nullable String collectedByUserId,
            String origin,
            Instant receivedAt,
            String receivedByUserId)
            implements SampleResponse {}
}
