package com.event.application.dto.attendance.command;

import lombok.Builder;

/**
 * 출석 응모 유스케이스 입력 DTO.
 *
 * Controller의 path/header 값을 그대로 서비스 전반에 흘리지 않고,
 * 유스케이스 실행에 필요한 최소 입력만 application 경계로 전달하기 위해 분리했다.
 * 공개 경계에서 원시 파라미터 나열이나 Entity 전달 대신 사용하는 것을 의도한다.
 */
@Builder
public record AttendEventCommand(
        Long eventId,
        Long roundId,
        Long memberId
) {

    public static AttendEventCommand of(Long eventId, Long roundId, Long memberId) {
        return AttendEventCommand.builder()
                .eventId(eventId)
                .roundId(roundId)
                .memberId(memberId)
                .build();
    }
}
