package com.event.presentation.controller;

import com.event.application.dto.attendance.AttendEventResult;
import com.event.application.dto.attendance.AttendanceSummaryDto;
import com.event.application.dto.attendance.AttendanceWinDto;
import com.event.application.port.input.AttendEventUseCase;
import com.event.domain.exception.BusinessException;
import com.event.domain.exception.code.EntryCode;
import com.event.domain.model.RewardType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventEntryController.class)
class EventEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttendEventUseCase attendEventUseCase;

    @Test
    void enterEvent_shouldReturnSuccess_whenHeaderExists() throws Exception {
        given(attendEventUseCase.attend(any())).willReturn(new AttendEventResult(
                200L,
                Instant.parse("2026-03-17T00:00:00Z"),
                2,
                true,
                new AttendanceWinDto(1L, "출석 포인트", RewardType.POINT, 30, null),
                new AttendanceSummaryDto(2, 28)
        ));

        mockMvc.perform(post("/event/v1/events/1/rounds/2/entries")
                        .header("X-Member-Id", "999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("출석 체크가 완료되었습니다."))
                .andExpect(jsonPath("$.data.entryId").value(200))
                .andExpect(jsonPath("$.data.win.rewardType").value("POINT"));
    }

    @Test
    void enterEvent_shouldReturnInvalidRequest_whenHeaderMissing() throws Exception {
        mockMvc.perform(post("/event/v1/events/1/rounds/2/entries")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.data.X-Member-Id").value("X-Member-Id 헤더는 필수입니다."));
    }

    @Test
    void enterEvent_shouldReturnDomainCode_whenBusinessExceptionOccurs() throws Exception {
        given(attendEventUseCase.attend(any()))
                .willThrow(BusinessException.from(EntryCode.ENTRY_ALREADY_APPLIED));

        mockMvc.perform(post("/event/v1/events/1/rounds/2/entries")
                        .header("X-Member-Id", "999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ENTRY_ALREADY_APPLIED"))
                .andExpect(jsonPath("$.message").value("이미 출석했습니다."));
    }
}
