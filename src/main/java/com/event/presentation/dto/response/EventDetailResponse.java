package com.event.presentation.dto.response;

import com.event.application.dto.event.EventDetailDto;
import com.event.application.dto.event.EventRoundDto;
import com.event.domain.model.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

/**
 * 출석 이벤트의 전체 회차 목록과 출석 상태를 조회
 * GET https://event-api.dolfarmer.com/event/v1/events/{eventId}
 * api 문서에 정해진 출석체크 참여 상태 조회 결과 dto
 * X-Member-Id 있음: 해당 회원의 출석 상태(ATTENDED / MISSED / TODAY / FUTURE) 포함
 */
@Schema(description = "출석 이벤트 상세 조회 응답")
@Builder
public record EventDetailResponse(
        @Schema(description = "이벤트 ID")
        Long eventId,
        @Schema(description = "이벤트명")
        String eventName,
        @Schema(description = "이벤트 유형")
        EventType eventType,
        @Schema(description = "이벤트 시작 시각")
        Instant startAt,
        @Schema(description = "이벤트 종료 시각")
        Instant endAt,
        @Schema(description = "공급사 ID")
        Long supplierId,
        @Schema(description = "이벤트 URL")
        String eventUrl,
        @Schema(description = "노출 우선순위")
        Integer priority,
        @Schema(description = "이벤트 활성화 여부")
        Boolean isActive,
        @Schema(description = "이벤트 노출 여부")
        Boolean isVisible,
        @Schema(description = "이벤트 설명")
        String description,
        @Schema(description = "전체 회차 수")
        Integer totalCount,
        @Schema(description = "이벤트 회차 목록. 데이터가 없어도 null 대신 빈 배열을 사용한다")
        List<EventRoundResponse> rounds,
        @Schema(description = "출석 요약 정보. 회원 식별 헤더가 없으면 null")
        AttendanceSummaryResponse attendanceSummary
) {

    public static EventDetailResponse from(EventDetailDto dto) {
        List<EventRoundDto> rounds = dto.rounds() == null ? List.of() : dto.rounds();
        List<EventRoundResponse> roundResponses = rounds
                .stream()
                .map(EventRoundResponse::from)
                .toList();

        return EventDetailResponse.builder()
                .eventId(dto.eventId())
                .eventName(dto.eventName())
                .eventType(dto.eventType())
                .startAt(dto.startAt())
                .endAt(dto.endAt())
                .supplierId(dto.supplierId())
                .eventUrl(dto.eventUrl())
                .priority(dto.priority())
                .isActive(dto.isActive())
                .isVisible(dto.isVisible())
                .description(dto.description())
                .totalCount(dto.totalCount())
                .rounds(roundResponses)
                .attendanceSummary(AttendanceSummaryResponse.from(dto.attendanceSummary()))
                .build();
    }
}
