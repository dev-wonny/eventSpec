package com.event.infrastructure.external.point.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class PointApiClientConfig {

    @Bean
    public RestClient pointRestClient(PointApiProperties pointApiProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) pointApiProperties.connectTimeout().toMillis());
        requestFactory.setReadTimeout((int) pointApiProperties.readTimeout().toMillis());

        return RestClient.builder()
                .baseUrl(pointApiProperties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}

