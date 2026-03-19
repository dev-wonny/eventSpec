package com.event.presentation.controller;

import com.event.application.dto.attendance.command.AttendEventCommand;
import com.event.application.port.input.AttendEventUseCase;
import com.event.presentation.dto.response.BaseResponse;
import com.event.presentation.dto.response.EventEntryResponse;
import com.event.presentation.resolver.MemberId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Attendance Entry")
@RestController
@RequiredArgsConstructor
@RequestMapping("/event/v1/events")
public class EventEntryController {

    private final AttendEventUseCase attendEventUseCase;

    @Operation(summary = "출석 이벤트 응모")
    @PostMapping("/{eventId}/rounds/{roundId}/entries")
    public BaseResponse<EventEntryResponse> enterEvent(
            @PathVariable Long eventId,
            @PathVariable Long roundId,
            @MemberId Long memberId
    ) {
        EventEntryResponse response = EventEntryResponse.from(
                attendEventUseCase.attend(AttendEventCommand.of(eventId, roundId, memberId))
        );

        return BaseResponse.success(response);
    }
}
