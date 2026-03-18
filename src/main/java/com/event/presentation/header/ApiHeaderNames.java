package com.event.presentation.header;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * presentation 계층에서 공통으로 사용하는 HTTP 헤더명 상수 모음.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiHeaderNames {

    public static final String X_MEMBER_ID = "X-Member-Id";
}
