package com.event.domain.util;

import java.time.ZoneId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AppTimeZones {

    public static final ZoneId ASIA_SEOUL = ZoneId.of("Asia/Seoul");
}
