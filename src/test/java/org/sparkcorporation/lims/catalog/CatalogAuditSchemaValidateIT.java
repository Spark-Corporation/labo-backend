package org.sparkcorporation.lims.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.sparkcorporation.lims.IntegrationTestBase;

/**
 * SP12: proves the catalog audit mapping validates against the schema. The {@code test} profile
 * runs Hibernate with {@code ddl-auto=validate}, so booting the context fails unless the four new
 * {@code *_aud} tables (analyses, analysis_components, analysis_techniques, reference_intervals)
 * match the {@code @Audited} entity mappings exactly. A bare context load is therefore the
 * assertion.
 */
@SpringBootTest
class CatalogAuditSchemaValidateIT extends IntegrationTestBase {

    @Test
    void contextLoads() {}
}
