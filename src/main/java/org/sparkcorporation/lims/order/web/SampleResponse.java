package org.sparkcorporation.lims.order.web;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "A sample collected in-lab, at the reception counter (type = IN_LAB).")
    record InLab(
            @Schema(description = "Unique identifier of the sample.") String id,
            @Schema(
                            description =
                                    "Daily sequence identifier encoded as the barcode on the"
                                            + " sample's printed label.")
                    String dailyId,
            @Schema(description = "Catalog code of the analysis this sample is for.")
                    String analysisCode,
            @Schema(description = "Code of the sample recipient (specimen container/type) used.")
                    String recipientCode,
            @Schema(description = "Timestamp at which the sample was collected.")
                    Instant collectedAt,
            @Schema(
                            description =
                                    "Identifier of the staff user who collected the sample, if"
                                            + " known.")
                    @Nullable String collectedByUserId)
            implements SampleResponse {}

    /** Home-collected sample with address / window / préleveur. */
    @Schema(description = "A sample collected at the patient's home by a Préleveur (type = HOME).")
    record Home(
            @Schema(description = "Unique identifier of the sample.") String id,
            @Schema(
                            description =
                                    "Daily sequence identifier encoded as the barcode on the"
                                            + " sample's printed label.")
                    String dailyId,
            @Schema(description = "Catalog code of the analysis this sample is for.")
                    String analysisCode,
            @Schema(description = "Code of the sample recipient (specimen container/type) used.")
                    String recipientCode,
            @Schema(description = "Timestamp at which the sample was collected.")
                    Instant collectedAt,
            @Schema(
                            description =
                                    "Identifier of the staff user who collected the sample, if"
                                            + " known.")
                    @Nullable String collectedByUserId,
            @Schema(description = "Patient's home address where the collection took place.")
                    String addressLine,
            @Schema(
                            description =
                                    "Distance in kilometers traveled by the Préleveur for this"
                                            + " collection.")
                    int distanceKm,
            @Schema(description = "Start of the scheduled collection time window.")
                    Instant windowStart,
            @Schema(description = "End of the scheduled collection time window.") Instant windowEnd,
            @Schema(
                            description =
                                    "Code of the Préleveur (home-collection agent) who performed"
                                            + " the collection.")
                    String preleveurCode)
            implements SampleResponse {}

    /** Externally-received sample with origin / reception metadata. */
    @Schema(
            description =
                    "A sample collected externally (at a partner site) and received at the lab"
                            + " (type = EXTERNAL_RECEIVED).")
    record ExternalReceived(
            @Schema(description = "Unique identifier of the sample.") String id,
            @Schema(
                            description =
                                    "Daily sequence identifier encoded as the barcode on the"
                                            + " sample's printed label.")
                    String dailyId,
            @Schema(description = "Catalog code of the analysis this sample is for.")
                    String analysisCode,
            @Schema(description = "Code of the sample recipient (specimen container/type) used.")
                    String recipientCode,
            @Schema(
                            description =
                                    "Timestamp at which the external provider collected the sample,"
                                            + " if reported.")
                    @Nullable Instant collectedAt,
            @Schema(
                            description =
                                    "Identifier of the staff user who collected the sample"
                                            + " externally, if known.")
                    @Nullable String collectedByUserId,
            @Schema(
                            description =
                                    "Free-text origin of the sample (the sending partner site or"
                                            + " provider).")
                    String origin,
            @Schema(description = "Timestamp at which the lab received the sample.")
                    Instant receivedAt,
            @Schema(
                            description =
                                    "Identifier of the staff user who received the sample at the"
                                            + " lab.")
                    String receivedByUserId)
            implements SampleResponse {}
}
