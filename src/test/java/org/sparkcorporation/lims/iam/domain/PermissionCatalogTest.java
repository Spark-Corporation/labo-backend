package org.sparkcorporation.lims.iam.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class PermissionCatalogTest {

    private static final Pattern NAME_FORMAT = Pattern.compile("^[a-z][a-z0-9.\\-]*$");

    @Test
    void contains_exactly_51_permissions() {
        assertThat(PermissionCatalog.ALL).hasSize(51);
    }

    @Test
    void all_names_match_format() {
        for (String p : PermissionCatalog.ALL) {
            assertThat(NAME_FORMAT.matcher(p).matches())
                    .as("permission %s must match %s", p, NAME_FORMAT)
                    .isTrue();
        }
    }

    @Test
    void includes_users_manage_and_audit_read() {
        assertThat(PermissionCatalog.ALL).contains("users.manage", "audit.read");
    }

    @Test
    void includes_audit_checkpoint() {
        assertThat(PermissionCatalog.ALL).contains("audit.checkpoint");
    }

    @Test
    void includes_lab_settings_read_and_manage() {
        assertThat(PermissionCatalog.ALL).contains("lab-settings.read", "lab-settings.manage");
    }

    @Test
    void includes_dossier_send() {
        assertThat(PermissionCatalog.ALL).contains("dossier.send");
    }

    @Test
    void includes_sample_destroy() {
        assertThat(PermissionCatalog.ALL).contains("sample.destroy");
    }

    @Test
    void includes_catalog_view_history() {
        assertThat(PermissionCatalog.ALL).contains("catalog.view-history");
    }
}
