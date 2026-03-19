package com.event.infrastructure.external.point.config;

import com.event.infrastructure.external.point.client.PointApiFeignClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.Request;
import feign.Target;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PointApiClientConfig {

    @Bean
    public PointApiFeignClient pointApiFeignClient(
            PointApiProperties pointApiProperties,
            ObjectMapper objectMapper
    ) {
        return Feign.builder()
                .encoder(new JacksonEncoder(objectMapper))
                .decoder(new JacksonDecoder(objectMapper))
                .options(new Request.Options(
                        pointApiProperties.connectTimeout().toMillis(),
                        TimeUnit.MILLISECONDS,
                        pointApiProperties.readTimeout().toMillis(),
                        TimeUnit.MILLISECONDS,
                        true
                ))
                // 실제 요청 URI는 메서드 파라미터로 받으므로 빈 target를 사용한다.
                .target(Target.EmptyTarget.create(PointApiFeignClient.class));
    }
}
