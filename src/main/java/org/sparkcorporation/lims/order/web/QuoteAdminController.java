package org.sparkcorporation.lims.order.web;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import dev.sparksam.securedx.rbac.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.sparkcorporation.lims.order.api.QuoteId;
import org.sparkcorporation.lims.order.api.QuoteLineId;
import org.sparkcorporation.lims.order.api.QuoteStatus;
import org.sparkcorporation.lims.order.domain.ImmutablePatientException;
import org.sparkcorporation.lims.order.domain.ImmutableQuoteNumberException;
import org.sparkcorporation.lims.order.domain.ImmutableStatusException;
import org.sparkcorporation.lims.order.domain.UpdateQuote;
import org.sparkcorporation.lims.order.infra.QuoteSendService;

/**
 * Admin REST controller for {@link org.sparkcorporation.lims.order.domain.Quote} per spec §7.1.
 *
 * <p>9 endpoints under {@code /api/v1/quotes/*}: standard CRUD ({@code POST}, {@code GET}, {@code
 * GET} list, {@code PATCH}, {@code DELETE}) plus line management ({@code POST /lines}, {@code
 * DELETE /lines/{lineId}}) and the two state-machine actions ({@code POST :lock}, {@code POST
 * :convert}). The colon-action endpoints render the {@code :name} suffix on the method mapping (not
 * the class mapping) so Spring's URL pattern matcher does not confuse the trailing fragment with a
 * path-segment continuation — same convention as {@link SampleAdminController}.
 *
 * <p>PATCH semantics (spec §5.4): the body binds as a {@link Map} so the immutable keys ({@code
 * quoteNumber}, {@code patientId}, {@code status}) can be detected and rejected with 422 before the
 * whitelisted keys ({@code prescriberId}, {@code expiresAt}) flow onto {@link UpdateQuote}.
 *
 * <p>{@code POST :convert} returns 201 with a {@link LabFileResponse} body and {@code Location:
 * /api/v1/lab-files/{newId}} — the new resource is the lab file, not the quote (RFC 7231 §6.3.2).
 */
@RestController
@RequestMapping("/api/v1/quotes")
@RequiredArgsConstructor
public class QuoteAdminController {

    private final QuoteService service;
    private final QuoteSendService quoteSendService;

    @PostMapping
    @Operation(
            summary = "Open a new quote",
            description =
                    "Opens a new price quote (devis) for a patient at status DRAFT, resolving the"
                            + " requested catalog analysis codes and bumping the quote-number"
                            + " counter. Returns 201 with a Location header pointing at the new"
                            + " quote.")
    @RequiresPermission("quote.create")
    public ResponseEntity<QuoteResponse> create(
            @Valid @RequestBody CreateQuoteRequest req, @Nullable Authentication authentication) {
        var resp = service.create(req, currentUserId(authentication));
        return ResponseEntity.created(URI.create("/api/v1/quotes/" + resp.id())).body(resp);
    }

    @GetMapping
    @Operation(
            summary = "Search quotes",
            description =
                    "Returns a paginated, filterable list of quotes. All filters are optional and"
                            + " combine as an AND; omitting a filter does not restrict on that"
                            + " dimension.")
    @RequiresPermission("quote.read")
    public QuoteSearchResponse list(
            @Parameter(description = "Restrict to quotes drawn up for this patient.")
                    @RequestParam(required = false)
                    @Nullable String patientId,
            @Parameter(description = "Restrict to quotes referencing this prescriber.")
                    @RequestParam(required = false)
                    @Nullable String prescriberId,
            @Parameter(description = "Restrict to quotes currently in this lifecycle status.")
                    @RequestParam(required = false)
                    @Nullable QuoteStatus status,
            @Parameter(description = "Restrict to quotes created on or after this instant.")
                    @RequestParam(required = false)
                    @Nullable Instant from,
            @Parameter(description = "Restrict to quotes created on or before this instant.")
                    @RequestParam(required = false)
                    @Nullable Instant to,
            @Parameter(description = "Zero-based page number to return.")
                    @RequestParam(defaultValue = "0")
                    @Min(0)
                    int page,
            @Parameter(description = "Number of items per page (max 100).")
                    @RequestParam(defaultValue = "20")
                    @Min(1)
                    @Max(100)
                    int size) {
        return service.search(patientId, prescriberId, status, from, to, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a quote", description = "Returns a single price quote (devis) by id.")
    @RequiresPermission("quote.read")
    public QuoteResponse get(@PathVariable String id) {
        return service.get(new QuoteId(id));
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Update a quote",
            description =
                    "Partially updates a DRAFT quote's mutable fields (prescriber, expiry date)."
                        + " quoteNumber, patientId and status are immutable via this endpoint and"
                        + " are rejected with 422 if present in the body; status only transitions"
                        + " via :lock / :convert.")
    @RequiresPermission("quote.create")
    public QuoteResponse update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        if (body.containsKey("quoteNumber")) {
            throw new ImmutableQuoteNumberException(
                    "quoteNumber is immutable; remove the 'quoteNumber' field from the PATCH body");
        }
        if (body.containsKey("patientId")) {
            throw new ImmutablePatientException(
                    "patientId is immutable; remove the 'patientId' field from the PATCH body");
        }
        if (body.containsKey("status")) {
            throw new ImmutableStatusException(
                    "status transitions only via :lock / :convert; remove the 'status' field"
                            + " from the PATCH body");
        }
        var cmd =
                new UpdateQuote(
                        asString(body.get("prescriberId")), asInstant(body.get("expiresAt")));
        return service.update(new QuoteId(id), cmd);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a quote", description = "Deletes a price quote (devis) by id.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("quote.create")
    public void delete(@PathVariable String id) {
        service.delete(new QuoteId(id));
    }

    @PostMapping("/{id}/lines")
    @Operation(
            summary = "Add lines to a quote",
            description =
                    "Appends one or more catalog analyses as new lines on a DRAFT quote. Each code"
                            + " is resolved against the analysis catalog and dedup-checked before"
                            + " persistence.")
    @RequiresPermission("quote.create")
    public QuoteResponse addLines(
            @PathVariable String id, @Valid @RequestBody AddQuoteLinesRequest req) {
        return service.addLines(new QuoteId(id), req.analyses());
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @Operation(
            summary = "Remove a quote line",
            description = "Removes a single analysis line from a DRAFT quote.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("quote.create")
    public void removeLine(@PathVariable String id, @PathVariable String lineId) {
        service.removeLine(new QuoteId(id), new QuoteLineId(lineId));
    }

    @PostMapping("/{id}:lock")
    @Operation(
            summary = "Lock and price a quote",
            description =
                    "Transitions a DRAFT quote to LOCKED: prices every line (indiciary value B ×"
                            + " rate × label count, in XOF) and freezes it against further edits.")
    @RequiresPermission("quote.lock")
    public QuoteResponse lock(@PathVariable String id) {
        return service.lock(new QuoteId(id));
    }

    @PostMapping("/{id}:send")
    @Operation(
            summary = "Send a quote to the patient",
            description =
                    "Sends a LOCKED quote to the patient over the requested notification channels."
                            + " Returns 202 with the ids of the enqueued notifications.")
    @RequiresPermission("quote.send")
    public ResponseEntity<QuoteSendResponse> send(
            @PathVariable String id, @Valid @RequestBody SendQuoteRequest req) {
        var ids = quoteSendService.send(new QuoteId(id), req.channels());
        return ResponseEntity.accepted().body(new QuoteSendResponse(ids));
    }

    @PostMapping("/{id}:convert")
    @Operation(
            summary = "Convert a quote into a lab file",
            description =
                    "Converts a LOCKED quote into a new lab file (dossier) at status OPEN, carrying"
                        + " over the quoted analyses and the requested urgent flag. Returns 201"
                        + " with a Location header pointing at the new lab file (the new resource"
                        + " is the lab file, not the quote).")
    @RequiresPermission("quote.create")
    public ResponseEntity<LabFileResponse> convert(
            @PathVariable String id,
            @Valid @RequestBody ConvertQuoteRequest req,
            @Nullable Authentication authentication) {
        var labFile = service.convert(new QuoteId(id), req, currentUserId(authentication));
        return ResponseEntity.created(URI.create("/api/v1/lab-files/" + labFile.id()))
                .body(labFile);
    }

    private static @Nullable String asString(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        throw new IllegalArgumentException(
                "expected string value, got " + value.getClass().getSimpleName());
    }

    private static @Nullable Instant asInstant(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return Instant.parse(s);
        }
        throw new IllegalArgumentException(
                "expected ISO-8601 instant string, got " + value.getClass().getSimpleName());
    }

    /**
     * Extracts the current user's id (the principal name) from the security context, or {@code
     * null} when unauthenticated. Mirrors the IAM module's pattern (e.g. {@code SelfController}).
     */
    private static @Nullable String currentUserId(@Nullable Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        var name = authentication.getName();
        return name == null || "anonymousUser".equals(name) ? null : name;
    }
}
