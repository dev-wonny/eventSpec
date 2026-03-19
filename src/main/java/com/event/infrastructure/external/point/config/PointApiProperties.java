package com.event.infrastructure.external.point.config;

import java.time.Duration;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.util.UriComponentsBuilder;

@ConfigurationProperties(prefix = "point-api")
public record PointApiProperties(
        String baseUrl,
        String grantPath,
        Duration connectTimeout,
        Duration readTimeout
) {

    public URI grantUri() {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path(grantPath)
                .build(true)
                .toUri();
    }
}
