package com.arenova.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Tournament settlement (ledger). UNDO: set arenova.settlement.enabled=false and restart.
 */
@Getter
@Configuration
public class SettlementProperties {

    @Value("${arenova.settlement.enabled:true}")
    private boolean enabled;
}
