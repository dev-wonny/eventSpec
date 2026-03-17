package com.event.presentation.controller;

import com.event.application.dto.event.GetEventDetailQuery;
import com.event.application.port.input.GetEventDetailUseCase;
import com.event.presentation.dto.response.BaseResponse;
import com.event.presentation.dto.response.EventDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Event")
@RestController
@RequiredArgsConstructor
@RequestMapping("/event/v1/events")
public class EventController {

    private final GetEventDetailUseCase getEventDetailUseCase;

    @Operation(summary = "출석 이벤트 상세 및 참여 상태 조회")
    @GetMapping("/{eventId}")
    public BaseResponse<EventDetailResponse> getEvent(
            @PathVariable Long eventId,
            @RequestHeader(value = "X-Member-Id", required = false) Long memberId
    ) {
        EventDetailResponse response = EventDetailResponse.from(
                getEventDetailUseCase.getEventDetail(GetEventDetailQuery.of(eventId, memberId))
        );

        return BaseResponse.success("이벤트를 조회했습니다.", response);
    }
}
