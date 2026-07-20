package org.sparkcorporation.lims;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LimsApplicationIT extends IntegrationTestBase {

    @Test
    void context_loads() {
        // Spring auto-wires every bean via the Postgres @ServiceConnection from
        // IntegrationTestBase. Liquibase runs against the container, JPA validates
        // the schema, Spring Modulith starts up. If any of those fail, the test
        // fails before reaching this method body.
    }
}
