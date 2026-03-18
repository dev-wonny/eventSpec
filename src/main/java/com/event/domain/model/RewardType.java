package com.event.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RewardType {
    POINT("포인트"),
    COUPON("쿠폰"),
    PRODUCT("실물 상품"),
    ETC("기타");

    private final String label;
}
