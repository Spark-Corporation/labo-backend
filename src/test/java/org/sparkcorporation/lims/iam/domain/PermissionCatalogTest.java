package org.sparkcorporation.lims.iam.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class PermissionCatalogTest {

    private static final Pattern NAME_FORMAT = Pattern.compile("^[a-z][a-z0-9.\\-]*$");

    @Test
    void contains_exactly_34_permissions() {
        assertThat(PermissionCatalog.ALL).hasSize(34);
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
}
