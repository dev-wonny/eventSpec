package com.event.application.dto.attendance.external;

import lombok.Builder;

/**
 * 포인트 지급 외부 연동용 명령 DTO.
 *
 * DB Entity나 HTTP 응답 형태에 맞추는 것이 아니라,
 * point API 호출에 필요한 값과 멱등키를 명확히 표현하기 위해 external 영역에 둔다.
 */
@Builder
public record PointGrantCommand(
        Long eventId,
        Long roundId,
        Long memberId,
        Long eventRoundPrizeId,
        Integer pointAmount,
        String idempotencyKey
) {

    public static PointGrantCommand of(
            Long eventId,
            Long roundId,
            Long memberId,
            Long eventRoundPrizeId,
            Integer pointAmount,
            String idempotencyKey
    ) {
        return PointGrantCommand.builder()
                .eventId(eventId)
                .roundId(roundId)
                .memberId(memberId)
                .eventRoundPrizeId(eventRoundPrizeId)
                .pointAmount(pointAmount)
                .idempotencyKey(idempotencyKey)
                .build();
    }
}
