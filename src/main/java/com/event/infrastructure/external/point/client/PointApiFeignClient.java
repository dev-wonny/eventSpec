package com.event.infrastructure.external.point.client;

import feign.Headers;
import feign.RequestLine;
import java.net.URI;

public interface PointApiFeignClient {

    @RequestLine("POST")
    @Headers({
            "Content-Type: application/json",
            "Accept: application/json"
    })
    PointGrantResponse grant(URI uri, PointGrantRequest request);
}
