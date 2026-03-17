package com.event.infrastructure.external.point;

import com.event.application.dto.attendance.PointGrantCommand;
import com.event.application.dto.attendance.PointGrantResult;
import com.event.application.port.output.PointRewardPort;
import com.event.infrastructure.external.point.client.PointApiFailedException;
import com.event.infrastructure.external.point.client.PointApiTimeoutException;
import com.event.infrastructure.external.point.client.PointGrantRequest;
import com.event.infrastructure.external.point.client.PointGrantResponse;
import com.event.infrastructure.external.point.config.PointApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class PointRewardAdapter implements PointRewardPort {

    private final RestClient pointRestClient;
    private final PointApiProperties pointApiProperties;

    @Override
    public PointGrantResult grant(PointGrantCommand command) {
        try {
            PointGrantResponse response = pointRestClient.post()
                    .uri(pointApiProperties.grantPath())
                    .body(PointGrantRequest.from(command))
                    .retrieve()
                    .body(PointGrantResponse.class);

            return PointGrantResult.from(response != null ? response.requestId() : null);
        } catch (ResourceAccessException ex) {
            throw PointApiTimeoutException.from("Point API timeout", ex);
        } catch (RestClientResponseException ex) {
            throw PointApiFailedException.from("Point API returned failure response", ex);
        } catch (RestClientException ex) {
            throw PointApiFailedException.from("Point API call failed", ex);
        }
    }
}
