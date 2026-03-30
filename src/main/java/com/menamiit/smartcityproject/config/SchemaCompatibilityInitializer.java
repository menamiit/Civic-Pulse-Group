package com.menamiit.smartcityproject.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SchemaCompatibilityInitializer {

    private static final Logger log = LoggerFactory.getLogger(SchemaCompatibilityInitializer.class);

    @Bean
    public CommandLineRunner ensureCompatibleSchema(JdbcTemplate jdbcTemplate) {
        return args -> {
            addColumnIfMissing(
                jdbcTemplate,
                "GRIEVANCES",
                "PRIORITY",
                "ALTER TABLE grievances ADD COLUMN priority VARCHAR(255) DEFAULT 'MEDIUM'"
            );
            jdbcTemplate.update("UPDATE grievances SET priority = 'MEDIUM' WHERE priority IS NULL");

            addColumnIfMissing(
                jdbcTemplate,
                "GRIEVANCES",
                "DUE_DATE",
                "ALTER TABLE grievances ADD COLUMN due_date DATE"
            );

            addColumnIfMissing(
                jdbcTemplate,
                "GRIEVANCES",
                "RESOLUTION_NOTES",
                "ALTER TABLE grievances ADD COLUMN resolution_notes VARCHAR(2000)"
            );

            addColumnIfMissing(
                jdbcTemplate,
                "GRIEVANCES",
                "RESOLUTION_IMAGE_PATHS",
                "ALTER TABLE grievances ADD COLUMN resolution_image_paths VARCHAR(4000)"
            );

            addColumnIfMissing(
                jdbcTemplate,
                "USERS",
                "DEPARTMENT",
                "ALTER TABLE users ADD COLUMN department VARCHAR(255)"
            );
        };
    }

    private void addColumnIfMissing(JdbcTemplate jdbcTemplate, String tableName, String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?",
            Integer.class,
            tableName,
            columnName
        );

        if (count != null && count == 0) {
            jdbcTemplate.execute(alterSql);
            log.info("Added missing column {}.{}", tableName, columnName);
        }
    }
}