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
                "CITIZEN_RATING",
                "ALTER TABLE grievances ADD COLUMN citizen_rating INTEGER"
            );

            addColumnIfMissing(
                jdbcTemplate,
                "GRIEVANCES",
                "CITIZEN_FEEDBACK",
                "ALTER TABLE grievances ADD COLUMN citizen_feedback VARCHAR(2000)"
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
                "GRIEVANCES",
                "LOW_RATING_REASON",
                "ALTER TABLE grievances ADD COLUMN low_rating_reason VARCHAR(1000)"
            );

            addColumnIfMissing(
                jdbcTemplate,
                "GRIEVANCES",
                "CITIZEN_REOPENED",
                "ALTER TABLE grievances ADD COLUMN citizen_reopened BOOLEAN DEFAULT FALSE"
            );
            jdbcTemplate.update("UPDATE grievances SET citizen_reopened = FALSE WHERE citizen_reopened IS NULL");

            addColumnIfMissing(
                jdbcTemplate,
                "GRIEVANCES",
                "REOPEN_REASON",
                "ALTER TABLE grievances ADD COLUMN reopen_reason VARCHAR(2000)"
            );

            addColumnIfMissing(
                jdbcTemplate,
                "GRIEVANCES",
                "REOPENED_AT",
                "ALTER TABLE grievances ADD COLUMN reopened_at TIMESTAMP"
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