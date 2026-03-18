package com.event.presentation.controller;

import com.event.application.dto.attendance.result.AttendanceSummaryDto;
import com.event.application.dto.event.EventDetailDto;
import com.event.application.dto.event.EventRoundDto;
import com.event.application.dto.event.GetEventDetailQuery;
import com.event.application.port.input.GetEventDetailUseCase;
import com.event.domain.model.AttendanceStatus;
import com.event.domain.model.EventType;
import com.event.presentation.header.ApiHeaderNames;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetEventDetailUseCase getEventDetailUseCase;

    @Test
    void getEvent_shouldAllowMissingMemberId() throws Exception {
        given(getEventDetailUseCase.getEventDetail(any(GetEventDetailQuery.class))).willReturn(new EventDetailDto(
                1L,
                "3월 출석 이벤트",
                EventType.ATTENDANCE,
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-03-31T23:59:59Z"),
                1L,
                "https://event",
                1,
                true,
                true,
                "description",
                2,
                List.of(
                        new EventRoundDto(10L, 1, LocalDate.parse("2026-03-01"), null, null),
                        new EventRoundDto(11L, 2, LocalDate.parse("2026-03-02"), null, null)
                ),
                null
        ));

        mockMvc.perform(get("/event/v1/events/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.rounds[0].status").value(nullValue()))
                .andExpect(jsonPath("$.data.rounds[0].win").value(nullValue()))
                .andExpect(jsonPath("$.data.supplierId").value(1));

        ArgumentCaptor<GetEventDetailQuery> captor = ArgumentCaptor.forClass(GetEventDetailQuery.class);
        verify(getEventDetailUseCase).getEventDetail(captor.capture());
        assertThat(captor.getValue().memberId()).isNull();
    }

    @Test
    void getEvent_shouldReturnStatus_whenMemberIdExists() throws Exception {
        given(getEventDetailUseCase.getEventDetail(any(GetEventDetailQuery.class))).willReturn(new EventDetailDto(
                1L,
                "3월 출석 이벤트",
                EventType.ATTENDANCE,
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-03-31T23:59:59Z"),
                1L,
                "https://event",
                1,
                true,
                true,
                "description",
                1,
                List.of(
                        new EventRoundDto(10L, 1, LocalDate.parse("2026-03-01"), AttendanceStatus.ATTENDED, null)
                ),
                new AttendanceSummaryDto(1, 28)
        ));

        mockMvc.perform(get("/event/v1/events/1")
                        .header(ApiHeaderNames.X_MEMBER_ID, "999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rounds[0].status").value("ATTENDED"))
                .andExpect(jsonPath("$.data.attendanceSummary.attendedDays").value(1));
    }
}
