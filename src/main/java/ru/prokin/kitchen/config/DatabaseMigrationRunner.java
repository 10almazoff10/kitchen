package ru.prokin.kitchen.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        cleanLegacyPollVoteConstraints();
    }

    private void cleanLegacyPollVoteConstraints() {
        DataSource dataSource = jdbcTemplate.getDataSource();
        if (dataSource == null) {
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String dbName = metaData.getDatabaseProductName();

            if (dbName != null && dbName.toLowerCase().contains("postgres")) {
                // 1. Прямое удаление старого известного констрейнта Hibernate
                try {
                    jdbcTemplate.execute("ALTER TABLE IF EXISTS restaurant_poll_votes DROP CONSTRAINT IF EXISTS ukcjcmmhiya8hfxut12rcpck7yv");
                    log.info("Checked/dropped legacy constraint ukcjcmmhiya8hfxut12rcpck7yv");
                } catch (Exception ex) {
                    log.debug("Direct drop of ukcjcmmhiya8hfxut12rcpck7yv skipped: {}", ex.getMessage());
                }

                // 2. Динамический поиск любых уникальных констрейнтов на (poll_id, user_id) без restaurant_id
                String sql = """
                    SELECT con.conname, pg_get_constraintdef(con.oid) AS def
                    FROM pg_constraint con
                    JOIN pg_class rel ON rel.oid = con.conrelid
                    WHERE rel.relname = 'restaurant_poll_votes' AND con.contype = 'u'
                """;
                List<Map<String, Object>> constraints = jdbcTemplate.queryForList(sql);
                for (Map<String, Object> c : constraints) {
                    String conName = (String) c.get("conname");
                    String def = (String) c.get("def");
                    if (def != null && def.contains("poll_id") && def.contains("user_id") && !def.contains("restaurant_id")) {
                        log.info("Found legacy unique constraint '{}' with definition '{}'. Dropping...", conName, def);
                        jdbcTemplate.execute("ALTER TABLE restaurant_poll_votes DROP CONSTRAINT IF EXISTS \"" + conName + "\"");
                        log.info("Successfully dropped legacy constraint '{}'", conName);
                    }
                }
            } else if (dbName != null && dbName.toLowerCase().contains("h2")) {
                try {
                    jdbcTemplate.execute("ALTER TABLE restaurant_poll_votes DROP CONSTRAINT IF EXISTS ukcjcmmhiya8hfxut12rcpck7yv");
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            log.warn("Notice during database constraint migration: {}", e.getMessage());
        }
    }
}
