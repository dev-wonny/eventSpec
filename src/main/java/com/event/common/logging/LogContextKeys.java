package com.event.common.logging;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LogContextKeys {

    public static final String REQUEST_ID = "requestId";
    public static final String COMMON_CODE = "commonCode";
    public static final String DOMAIN_CODE = "domainCode";
    public static final String EVENT_ID = "eventId";
    public static final String ROUND_ID = "roundId";
    public static final String MEMBER_ID = "memberId";
}

