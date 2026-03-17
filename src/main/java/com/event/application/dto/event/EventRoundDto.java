package com.event.application.dto.event;

import com.event.domain.model.AttendanceStatus;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record EventRoundDto(
        Long roundId,
        Integer roundNo,
        LocalDate roundDate,
        AttendanceStatus status,
        EventWinInfoDto win
) {

    public static EventRoundDto of(
            Long roundId,
            Integer roundNo,
            LocalDate roundDate,
            AttendanceStatus status,
            EventWinInfoDto win
    ) {
        return EventRoundDto.builder()
                .roundId(roundId)
                .roundNo(roundNo)
                .roundDate(roundDate)
                .status(status)
                .win(win)
                .build();
    }
}
