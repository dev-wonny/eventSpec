package com.event.infrastructure.external.point.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "point-api")
public record PointApiProperties(
        String baseUrl,
        String grantPath,
        Duration connectTimeout,
        Duration readTimeout
) {
}

