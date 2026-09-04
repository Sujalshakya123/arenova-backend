package com.arenova.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Prize pool funding model.
 * UNDO Option B: set arenova.prize-pool.mode=fixed in application.properties and restart.
 */
@Getter
@Configuration
public class PrizePoolProperties {

    @Value("${arenova.prize-pool.mode:entry-fee-funded}")
    private String mode;

    @Value("${arenova.prize-pool.prize-percent:70}")
    private int prizePercent;

    @Value("${arenova.prize-pool.organizer-percent:20}")
    private int organizerPercent;

    @Value("${arenova.prize-pool.platform-percent:10}")
    private int platformPercent;

    /** % of total collected revenue (e.g. 40 = 1st place gets 40% of revenue). */
    @Value("${arenova.prize-pool.first-place-percent:40}")
    private int firstPlacePercent;

    /** % of total collected revenue (e.g. 30 = 2nd place gets 30% of revenue). */
    @Value("${arenova.prize-pool.second-place-percent:30}")
    private int secondPlacePercent;

    public boolean isEntryFeeFunded() {
        return "entry-fee-funded".equalsIgnoreCase(mode);
    }
}
