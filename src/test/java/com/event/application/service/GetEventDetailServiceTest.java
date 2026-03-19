package com.event.application.service;

import com.event.application.dto.event.EventDetailDto;
import com.event.application.dto.event.GetEventDetailQuery;
import com.event.application.port.output.EventEntryRepositoryPort;
import com.event.application.port.output.EventQueryPort;
import com.event.application.port.output.EventRoundPrizeQueryPort;
import com.event.application.port.output.EventRoundQueryPort;
import com.event.application.port.output.EventWinQueryPort;
import com.event.application.port.output.PrizeQueryPort;
import com.event.domain.entity.EventEntity;
import com.event.domain.entity.EventRoundEntity;
import com.event.domain.entity.EventRoundPrizeEntity;
import com.event.domain.entity.EventWinEntity;
import com.event.domain.entity.PrizeEntity;
import com.event.domain.model.AttendanceStatus;
import com.event.domain.model.EventType;
import com.event.domain.model.RewardType;
import com.event.domain.util.AppTimeZones;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetEventDetailServiceTest {

    @Mock
    private EventQueryPort eventQueryPort;

    @Mock
    private EventRoundQueryPort eventRoundQueryPort;

    @Mock
    private EventEntryRepositoryPort eventEntryRepositoryPort;

    @Mock
    private EventWinQueryPort eventWinQueryPort;

    @Mock
    private EventRoundPrizeQueryPort eventRoundPrizeQueryPort;

    @Mock
    private PrizeQueryPort prizeQueryPort;

    @InjectMocks
    private GetEventDetailService getEventDetailService;

    private EventEntity event;
    private EventRoundEntity attendedRound;
    private EventRoundEntity missedRound;
    private EventRoundEntity todayRound;
    private EventRoundEntity futureRound;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now(AppTimeZones.ASIA_SEOUL);

        event = EventEntity.builder()
                .id(1L)
                .eventName("3월 출석 이벤트")
                .eventType(EventType.ATTENDANCE)
                .startAt(today.minusDays(5).atStartOfDay(AppTimeZones.ASIA_SEOUL).toInstant())
                .endAt(today.plusDays(5).atStartOfDay(AppTimeZones.ASIA_SEOUL).toInstant())
                .supplierId(10L)
                .eventUrl("https://event")
                .priority(1)
                .isActive(true)
                .isVisible(true)
                .isAutoEntry(false)
                .isSnsLinked(false)
                .isWinnerAnnounced(false)
                .isDuplicateWinner(false)
                .isMultipleEntry(false)
                .description("attendance")
                .build();

        attendedRound = round(101L, 1, today.minusDays(2));
        missedRound = round(102L, 2, today.minusDays(1));
        todayRound = round(103L, 3, today);
        futureRound = round(104L, 4, today.plusDays(1));
    }

    @Test
    void getEventDetail_shouldReturnStatusAndWin_whenMemberIdExists() {
        EventWinEntity win = EventWinEntity.create(201L, attendedRound, 999L, 301L, 999L);
        EventRoundPrizeEntity roundPrize = EventRoundPrizeEntity.builder()
                .id(301L)
                .round(attendedRound)
                .prizeId(401L)
                .priority(0)
                .isActive(true)
                .build();
        PrizeEntity prize = PrizeEntity.builder()
                .id(401L)
                .prizeName("출석 포인트")
                .rewardType(RewardType.POINT)
                .pointAmount(30)
                .isActive(true)
                .build();

        when(eventQueryPort.findById(1L)).thenReturn(Optional.of(event));
        when(eventRoundQueryPort.findAllByEventId(1L)).thenReturn(List.of(attendedRound, missedRound, todayRound, futureRound));
        when(eventEntryRepositoryPort.findAttendedRoundIdsByEventIdAndMemberId(1L, 999L)).thenReturn(Set.of(101L));
        when(eventWinQueryPort.findByEventIdAndMemberId(1L, 999L)).thenReturn(List.of(win));
        when(eventRoundPrizeQueryPort.findByIds(java.util.Set.of(301L))).thenReturn(Map.of(301L, roundPrize));
        when(prizeQueryPort.findByIds(java.util.Set.of(401L))).thenReturn(Map.of(401L, prize));

        EventDetailDto result = getEventDetailService.getEventDetail(new GetEventDetailQuery(1L, 999L));

        assertThat(result.attendanceSummary()).isNotNull();
        assertThat(result.attendanceSummary().attendedDays()).isEqualTo(1);
        assertThat(result.rounds()).hasSize(4);
        assertThat(result.rounds().get(0).status()).isEqualTo(AttendanceStatus.ATTENDED);
        assertThat(result.rounds().get(0).win()).isNotNull();
        assertThat(result.rounds().get(1).status()).isEqualTo(AttendanceStatus.MISSED);
        assertThat(result.rounds().get(2).status()).isEqualTo(AttendanceStatus.TODAY);
        assertThat(result.rounds().get(3).status()).isEqualTo(AttendanceStatus.FUTURE);
    }

    @Test
    void getEventDetail_shouldReturnNullStatusAndSummary_whenMemberIdIsAbsent() {
        when(eventQueryPort.findById(1L)).thenReturn(Optional.of(event));
        when(eventRoundQueryPort.findAllByEventId(1L)).thenReturn(List.of(attendedRound, missedRound));

        EventDetailDto result = getEventDetailService.getEventDetail(new GetEventDetailQuery(1L, null));

        assertThat(result.attendanceSummary()).isNull();
        assertThat(result.rounds()).allMatch(round -> Objects.isNull(round.status()) && Objects.isNull(round.win()));
    }

    private EventRoundEntity round(Long id, int roundNo, LocalDate roundDate) {
        Instant start = roundDate.atStartOfDay(AppTimeZones.ASIA_SEOUL).toInstant();
        Instant end = roundDate.plusDays(1).atStartOfDay(AppTimeZones.ASIA_SEOUL).minusSeconds(1).toInstant();
        return EventRoundEntity.builder()
                .id(id)
                .event(event)
                .roundNo(roundNo)
                .roundStartAt(start)
                .roundEndAt(end)
                .isConfirmed(false)
                .build();
    }
}
