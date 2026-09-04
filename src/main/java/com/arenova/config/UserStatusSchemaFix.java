package com.arenova.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures user.status accepts PENDING/REJECTED and migrates legacy organizer
 * INACTIVE rows to PENDING for the approval workflow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserStatusSchemaFix implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.update(
                    "UPDATE `user` SET `status` = 'ACTIVE' WHERE `status` IS NULL OR `status` = ''"
            );
        } catch (Exception e) {
            log.debug("user.status cleanup skipped: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute(
                    "ALTER TABLE `user` MODIFY COLUMN `status` "
                            + "ENUM('ACTIVE','PENDING','REJECTED','INACTIVE','SUSPENDED') "
                            + "NOT NULL DEFAULT 'ACTIVE'"
            );
            log.info("Expanded user.status ENUM with PENDING/REJECTED");
        } catch (Exception e) {
            log.warn("user.status ENUM expansion failed: {} — trying VARCHAR", e.getMessage());
            try {
                jdbcTemplate.execute(
                        "ALTER TABLE `user` MODIFY COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'"
                );
                log.info("Updated user.status to VARCHAR(20)");
            } catch (Exception varcharError) {
                log.warn("user.status VARCHAR migration failed: {}", varcharError.getMessage());
            }
        }

        try {
            int migrated = jdbcTemplate.update(
                    "UPDATE `user` SET `status` = 'PENDING' WHERE `role` = 'ORGANIZER' AND `status` = 'INACTIVE'"
            );
            if (migrated > 0) {
                log.info("Migrated {} organizer(s) from INACTIVE to PENDING", migrated);
            }
        } catch (Exception e) {
            log.warn("organizer INACTIVE->PENDING migration failed: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute(
                    "ALTER TABLE `user` ADD COLUMN `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP"
            );
            log.info("Added user.created_at column");
        } catch (Exception e) {
            log.debug("user.created_at migration skipped: {}", e.getMessage());
        }
    }
}
