package org.sparkcorporation.lims.iam.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.iam.api.UserApi;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.shared.id.Ulid;

@Tag("integration")
@SpringBootTest
class UserApiFindAllByIdsIT extends IntegrationTestBase {

    @Autowired private UserApi userApi;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void returnsRequestedSubset_absentIdsExcluded() {
        UserId admin =
                new UserId(
                        jdbcTemplate.queryForObject(
                                "SELECT id FROM users WHERE email = 'admin@test.local'",
                                String.class));
        UserId cashier =
                new UserId(
                        jdbcTemplate.queryForObject(
                                "SELECT id FROM users WHERE email = 'cashier@test.local'",
                                String.class));
        UserId nonexistent = new UserId(Ulid.generate());

        var result = userApi.findAllByIds(Set.of(admin, cashier, nonexistent));

        assertThat(result).containsOnlyKeys(admin, cashier);
        assertThat(result.get(admin).email()).isEqualTo("admin@test.local");
        assertThat(result.get(cashier).enabled()).isTrue();
    }

    @Test
    void emptyInputReturnsEmptyMap() {
        assertThat(userApi.findAllByIds(Set.of())).isEmpty();
    }
}
