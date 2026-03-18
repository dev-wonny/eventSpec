package com.event.presentation.dto.response;

import com.event.application.dto.event.EventRoundDto;
import com.event.domain.model.AttendanceStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Builder;

@Schema(description = "이벤트 회차 응답")
@Builder
public record EventRoundResponse(
        @Schema(description = "회차 ID")
        Long roundId,
        @Schema(description = "회차 번호")
        Integer roundNo,
        @Schema(description = "회차 날짜")
        LocalDate roundDate,
        @Schema(description = "회원 출석 상태. 회원 식별 헤더가 없으면 null")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        AttendanceStatus status,
        @Schema(description = "회차 당첨 정보. 미당첨 시 null")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        EventRoundWinResponse win
) {

    public static EventRoundResponse from(EventRoundDto dto) {
        return EventRoundResponse.builder()
                .roundId(dto.roundId())
                .roundNo(dto.roundNo())
                .roundDate(dto.roundDate())
                .status(dto.status())
                .win(EventRoundWinResponse.from(dto.win()))
                .build();
    }
}
