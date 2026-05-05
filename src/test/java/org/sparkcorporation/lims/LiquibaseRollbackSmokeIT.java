package org.sparkcorporation.lims;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LiquibaseRollbackSmokeIT extends IntegrationTestBase {

    @Autowired private DataSource dataSource;

    @Test
    void update_rollback_update_cycle_runs_clean() throws Exception {
        try (var connection = dataSource.getConnection()) {
            Database database =
                    DatabaseFactory.getInstance()
                            .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName("lims");
            database.setLiquibaseSchemaName("lims_liquibase");

            try (Liquibase liquibase =
                    new Liquibase(
                            "db/changelog/master.changelog.yaml",
                            new ClassLoaderResourceAccessor(),
                            database)) {

                liquibase.rollback(1, new Contexts(), new LabelExpression());
                liquibase.update(new Contexts(), new LabelExpression());
            }

            try (var stmt = connection.createStatement();
                    var rs =
                            stmt.executeQuery(
                                    "SELECT count(*) FROM information_schema.schemata WHERE"
                                            + " schema_name = 'lims'")) {
                rs.next();
                assertThat(rs.getInt(1))
                        .as("lims schema exists after update -> rollback -> update cycle")
                        .isEqualTo(1);
            }
        }
    }
}
