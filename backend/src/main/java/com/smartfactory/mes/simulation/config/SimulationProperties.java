package com.smartfactory.mes.simulation.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mes.simulation")
public class SimulationProperties {

    private boolean enabled = true;
    private long fixedDelayMs = 5000L;
    private long initialDelayMs = 5000L;
    private int bootstrapTicks = 12;
    private int dashboardTrendHours = 8;
}
