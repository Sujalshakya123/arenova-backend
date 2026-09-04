package com.arenova.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Organizer reports + enhanced admin settlement filters.
 * UNDO: set arenova.reports.enabled=false and restart backend.
 */
@Getter
@Configuration
public class ReportsProperties {

    @Value("${arenova.reports.enabled:true}")
    private boolean enabled;
}
