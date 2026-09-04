package com.arenova.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Task 8 added PENDING/REJECTED registration statuses. Older MySQL schemas
 * stored status as ENUM('REGISTERED','WITHDRAWN') which rejects PENDING.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationStatusSchemaFix implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE event_registrations MODIFY COLUMN status VARCHAR(20) NOT NULL"
            );
            log.info("Updated event_registrations.status to VARCHAR(20)");
        } catch (Exception e) {
            log.debug("event_registrations.status migration skipped: {}", e.getMessage());
        }
    }
}
