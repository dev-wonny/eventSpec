package com.event.infrastructure.config;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeZoneConfig {

    @Value("${app.time-zone:Asia/Seoul}")
    private String timeZoneId;

    @PostConstruct
    void setUpTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(timeZoneId));
    }
}
