package com.event.presentation.dto.response;

import com.event.application.dto.event.EventDetailDto;
import com.event.application.dto.event.EventRoundDto;
import com.event.domain.model.AttendanceStatus;
import com.event.domain.model.EventType;
import com.event.domain.model.RewardType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 출석 이벤트의 전체 회차 목록과 출석 상태를 조회
 * GET https://event-api.dolfarmer.com/event/v1/events/{eventId}
 * api 문서에 정해진 출석체크 참여 상태 조회 결과 dto
 * X-Member-Id 있음: 해당 회원의 출석 상태(ATTENDED / MISSED / TODAY / FUTURE) 포함
 */
@Builder
public record EventDetailResponse(
        Long eventId,
        String eventName,
        EventType eventType,
        Instant startAt,
        Instant endAt,
        Long supplierId,
        String eventUrl,
        Integer priority,
        Boolean isActive,
        Boolean isVisible,
        String description,
        Integer totalCount,
        List<RoundResponse> rounds,
        AttendanceSummaryResponse attendanceSummary
) {

    public static EventDetailResponse from(EventDetailDto dto) {
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
                .rounds(dto.rounds().stream().map(RoundResponse::from).toList())
                .attendanceSummary(AttendanceSummaryResponse.from(dto))
                .build();
    }

    @Builder
    public record RoundResponse(
            Long roundId,
            Integer roundNo,
            LocalDate roundDate,
            @JsonInclude(JsonInclude.Include.ALWAYS)
            AttendanceStatus status,
            @JsonInclude(JsonInclude.Include.ALWAYS)
            WinResponse win
    ) {

        public static RoundResponse from(EventRoundDto dto) {
            return RoundResponse.builder()
                    .roundId(dto.roundId())
                    .roundNo(dto.roundNo())
                    .roundDate(dto.roundDate())
                    .status(dto.status())
                    .win(WinResponse.from(dto))
                    .build();
        }
    }

    @Builder
    public record WinResponse(
            String prizeName,
            RewardType rewardType,
            Integer pointAmount
    ) {

        public static WinResponse from(EventRoundDto dto) {
            if (dto.win() == null) {
                return null;
            }

            return WinResponse.builder()
                    .prizeName(dto.win().prizeName())
                    .rewardType(dto.win().rewardType())
                    .pointAmount(dto.win().pointAmount())
                    .build();
        }
    }

    @Builder
    public record AttendanceSummaryResponse(
            Integer attendedDays,
            Integer totalDays
    ) {

        public static AttendanceSummaryResponse from(EventDetailDto dto) {
            if (dto.attendanceSummary() == null) {
                return null;
            }

            return AttendanceSummaryResponse.builder()
                    .attendedDays(dto.attendanceSummary().attendedDays())
                    .totalDays(dto.attendanceSummary().totalDays())
                    .build();
        }
    }
}
