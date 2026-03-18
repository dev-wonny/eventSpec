package com.event.application.dto.attendance.internal;

import com.event.application.dto.attendance.external.PointGrantCommand;
import com.event.application.dto.attendance.result.AttendEventResult;
import lombok.Builder;

/**
 * 출석 트랜잭션 내부 조립 결과 DTO.
 *
 * 유스케이스 최종 결과와 post-commit 외부 연동 명령을 함께 묶어
 * Service 내부 흐름을 정리하려는 목적이라 internal 영역에 둔다.
 */
@Builder
public record AttendEventTransactionResult(
        AttendEventResult attendEventResult,
        PointGrantCommand pointGrantCommand
) {

    public static AttendEventTransactionResult of(
            AttendEventResult attendEventResult,
            PointGrantCommand pointGrantCommand
    ) {
        return AttendEventTransactionResult.builder()
                .attendEventResult(attendEventResult)
                .pointGrantCommand(pointGrantCommand)
                .build();
    }
}
