package com.event.infrastructure.external.point;

import com.event.application.dto.attendance.external.PointGrantCommand;
import com.event.application.dto.attendance.external.PointGrantResult;
import com.event.application.port.output.PointRewardPort;
import com.event.infrastructure.external.point.client.PointApiFeignClient;
import com.event.infrastructure.external.point.client.PointGrantRequest;
import com.event.infrastructure.external.point.client.PointGrantResponse;
import com.event.infrastructure.external.point.config.PointApiProperties;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Feign 기반 외부 point API 호출 adapter.
 *
 * application은 PointRewardPort만 알고, 실제 HTTP 호출은 이 구현체가 맡는다.
 */
@Component
@RequiredArgsConstructor
public class PointRewardImpl implements PointRewardPort {

    private final PointApiFeignClient pointApiFeignClient;
    private final PointApiProperties pointApiProperties;

    @Override
    public PointGrantResult grant(PointGrantCommand command) {
        PointGrantResponse response = pointApiFeignClient.grant(
                pointApiProperties.grantUri(),
                PointGrantRequest.from(command)
        );
        return PointGrantResult.from(Objects.nonNull(response) ? response.requestId() : null);
    }
}
