package org.sparkcorporation.lims.catalog.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Fail-fast bootstrap guard for the {@code catalog_code_counters} table.
 *
 * <p>If the seed changeset {@code 202605061600-seed-catalog-code-counters} does not apply (e.g.
 * mismatched Liquibase contexts), the first {@code POST /api/v1/catalog/prescribers} would throw
 * {@link IllegalStateException} from {@link CatalogCodeGenerator#next} and surface as 500. This
 * listener verifies both counter rows exist on {@link ApplicationReadyEvent} and throws otherwise,
 * so the failure surfaces at boot rather than at first request — see spec §4.6.1.
 */
@Component
@RequiredArgsConstructor
class CatalogCounterStartupCheck implements ApplicationListener<ApplicationReadyEvent> {

    private final CatalogCodeCounterRepository counters;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        for (var type : CatalogCodeGenerator.EntityType.values()) {
            counters.findById(type.name())
                    .orElseThrow(
                            () ->
                                    new IllegalStateException(
                                            "catalog_code_counters row missing for "
                                                    + type
                                                    + " — seed changeset 202605061600 did not"
                                                    + " apply"));
        }
    }
}
