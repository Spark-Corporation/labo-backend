package org.sparkcorporation.lims.iam;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.iam.domain.PermissionCatalog;

@Tag("integration")
@SpringBootTest
class IamCatalogIntegrityTest extends IntegrationTestBase {

    @Autowired private EntityManager em;

    @Test
    void permissionCatalog_matches_database_seed() {
        Set<String> dbNames =
                em.createQuery("SELECT p.name FROM PermissionEntity p", String.class)
                        .getResultStream()
                        .collect(Collectors.toUnmodifiableSet());
        assertThat(PermissionCatalog.ALL).isEqualTo(dbNames);
    }
}
