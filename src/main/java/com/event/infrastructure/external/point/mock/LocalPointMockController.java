package com.event.infrastructure.external.point.mock;

import com.event.infrastructure.external.point.client.PointGrantRequest;
import com.event.infrastructure.external.point.client.PointGrantResponse;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Hidden
@Profile("local")
@RestController
@RequestMapping("/mock/points")
public class LocalPointMockController {

    @PostMapping("/grants")
    public PointGrantResponse grant(@RequestBody PointGrantRequest request) {
        String requestId = "mock-" + UUID.randomUUID().toString().replace("-", "");

        log.info(
                "localPointMock memberId={} pointAmount={} idempotencyKey={} requestId={}",
                request.memberId(),
                request.pointAmount(),
                request.idempotencyKey(),
                requestId
        );

        return PointGrantResponse.of(requestId);
    }
}
