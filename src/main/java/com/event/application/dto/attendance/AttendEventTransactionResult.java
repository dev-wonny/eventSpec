package com.event.application.dto.attendance;

import lombok.Builder;

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
