package com.smartfactory.mes.global.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class ApplicationTimeZoneConfig {

    private final String timeZoneId;

    public ApplicationTimeZoneConfig(
            @Value("${app.timezone:Asia/Seoul}") String timeZoneId
    ) {
        this.timeZoneId = timeZoneId;
    }

    @PostConstruct
    public void applyDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(timeZoneId));
    }
}
