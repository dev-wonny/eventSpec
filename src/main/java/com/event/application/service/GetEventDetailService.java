package com.event.application.service;

import com.event.application.dto.attendance.result.AttendanceSummaryDto;
import com.event.application.dto.event.EventDetailDto;
import com.event.application.dto.event.EventRoundDto;
import com.event.application.dto.event.EventWinInfoDto;
import com.event.application.dto.event.GetEventDetailQuery;
import com.event.application.port.input.GetEventDetailUseCase;
import com.event.application.port.output.EventEntryQueryPort;
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
import com.event.domain.exception.BusinessException;
import com.event.domain.exception.code.EventCode;
import com.event.domain.model.AttendanceStatus;
import com.event.domain.model.EventType;
import com.event.domain.util.AppTimeZones;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이벤트 상세 조회 유스케이스 서비스.
 *
 * memberId가 없으면 이벤트와 회차 기본 정보만 반환하고,
 * memberId가 있으면 출석 상태와 당첨 정보를 함께 조립한다.
 */
@Service
@RequiredArgsConstructor
public class GetEventDetailService implements GetEventDetailUseCase {

    private final EventQueryPort eventQueryPort;
    private final EventRoundQueryPort eventRoundQueryPort;
    private final EventEntryQueryPort eventEntryQueryPort;
    private final EventWinQueryPort eventWinQueryPort;
    private final EventRoundPrizeQueryPort eventRoundPrizeQueryPort;
    private final PrizeQueryPort prizeQueryPort;

    @Override
    @Transactional(readOnly = true)
    public EventDetailDto getEventDetail(GetEventDetailQuery query) {
        // 1. 이벤트를 조회하고 출석 이벤트인지 확인한다.
        EventEntity event = eventQueryPort.findById(query.eventId())
                .orElseThrow(() -> BusinessException.from(EventCode.EVENT_NOT_FOUND));

        if (event.getEventType() != EventType.ATTENDANCE) {
            throw BusinessException.from(EventCode.EVENT_NOT_ATTENDANCE);
        }

        List<EventRoundEntity> rounds = eventRoundQueryPort.findAllByEventId(query.eventId());

        if (query.memberId() == null) {
            // 2-1. 회원 식별 정보가 없으면 회차 기본 정보만 조립한다.
            return EventDetailDto.of(
                    event,
                    rounds.stream()
                            .map(round -> EventRoundDto.of(
                                    round.getId(),
                                    round.getRoundNo(),
                                    resolveRoundDate(event, round),
                                    null,
                                    null
                            ))
                            .toList(),
                    null
            );
        }

        // 2-2. 회원 식별 정보가 있으면 출석 상태와 당첨 정보를 함께 계산한다.
        List<EventWinEntity> wins = eventWinQueryPort.findByEventIdAndMemberId(query.eventId(), query.memberId());

        Set<Long> attendedRoundIds = eventEntryQueryPort.findAttendedRoundIdsByEventIdAndMemberId(
                query.eventId(),
                query.memberId()
        );

        // 회차별 당첨 정보 결합을 쉽게 하기 위해 roundId 기준 맵으로 바꿔 둔다.
        Map<Long, EventWinEntity> winByRoundId = wins.stream()
                .collect(Collectors.toMap(
                        EventWinEntity::getRoundId,
                        Function.identity(),
                        (first, second) -> first
                ));

        Map<Long, EventRoundPrizeEntity> eventRoundPrizeMap = eventRoundPrizeQueryPort.findByIds(extractRoundPrizeIds(wins));
        Map<Long, PrizeEntity> prizeMap = prizeQueryPort.findByIds(extractPrizeIds(eventRoundPrizeMap.values()));

        LocalDate today = LocalDate.now(AppTimeZones.ASIA_SEOUL);
        // 3. 각 회차를 응답용 DTO로 변환하면서 상태와 당첨 정보를 붙인다.
        List<EventRoundDto> roundDtos = rounds.stream()
                .map(round -> {
                    EventWinEntity win = winByRoundId.get(round.getId());
                    EventWinInfoDto winInfo = buildWinInfo(win, eventRoundPrizeMap, prizeMap);
                    return EventRoundDto.of(
                            round.getId(),
                            round.getRoundNo(),
                            resolveRoundDate(event, round),
                            resolveStatus(round, attendedRoundIds, today, event),
                            winInfo
                    );
                })
                .toList();

        // 4. 회차 목록과 누적 출석 요약을 함께 반환한다.
        return EventDetailDto.of(
                event,
                roundDtos,
                AttendanceSummaryDto.of(attendedRoundIds.size(), roundDtos.size())
        );
    }

    private Set<Long> extractRoundPrizeIds(List<EventWinEntity> wins) {
        // 실제 당첨 데이터가 연결한 prize 매핑 ID만 뽑아 한 번에 조회한다.
        return wins.stream()
                .map(EventWinEntity::getEventRoundPrizeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<Long> extractPrizeIds(Collection<EventRoundPrizeEntity> eventRoundPrizes) {
        // prize 본문 조회도 ID만 모아서 배치로 처리한다.
        return eventRoundPrizes.stream()
                .map(EventRoundPrizeEntity::getPrizeId)
                .collect(Collectors.toSet());
    }

    private EventWinInfoDto buildWinInfo(
            EventWinEntity win,
            Map<Long, EventRoundPrizeEntity> eventRoundPrizeMap,
            Map<Long, PrizeEntity> prizeMap
    ) {
        // 당첨 정보가 없으면 응답의 win 필드는 null이다.
        if (win == null || win.getEventRoundPrizeId() == null) {
            return null;
        }

        // 연결 테이블이 사라졌거나 비정상이면 당첨 정보 조립을 생략한다.
        EventRoundPrizeEntity eventRoundPrize = eventRoundPrizeMap.get(win.getEventRoundPrizeId());
        if (eventRoundPrize == null) {
            return null;
        }

        // 실제 prize 정보가 없으면 노출할 수 없으므로 null 처리한다.
        PrizeEntity prize = prizeMap.get(eventRoundPrize.getPrizeId());
        if (prize == null) {
            return null;
        }

        return EventWinInfoDto.of(
                prize.getPrizeName(),
                prize.getRewardType(),
                prize.getPointAmount()
        );
    }

    private AttendanceStatus resolveStatus(
            EventRoundEntity round,
            Set<Long> attendedRoundIds,
            LocalDate today,
            EventEntity event
    ) {
        // 이미 출석한 회차가 최우선이다.
        if (attendedRoundIds.contains(round.getId())) {
            return AttendanceStatus.ATTENDED;
        }

        LocalDate roundDate = resolveRoundDate(event, round);
        // 아직 출석 전인 회차는 오늘/미래/놓침으로 나눠 상태를 계산한다.
        if (roundDate.isEqual(today)) {
            return AttendanceStatus.TODAY;
        }
        if (roundDate.isAfter(today)) {
            return AttendanceStatus.FUTURE;
        }
        return AttendanceStatus.MISSED;
    }

    private LocalDate resolveRoundDate(EventEntity event, EventRoundEntity round) {
        // 회차 개별 시작일이 있으면 그 값을 우선한다.
        if (round.getRoundStartAt() != null) {
            return round.getRoundStartAt().atZone(AppTimeZones.ASIA_SEOUL).toLocalDate();
        }

        // round_start_at이 없으면 이벤트 시작일과 roundNo를 기준으로 회차 날짜를 계산한다.
        Instant baseStart = event.getStartAt().plusSeconds((long) (round.getRoundNo() - 1) * 24 * 60 * 60);
        return baseStart.atZone(AppTimeZones.ASIA_SEOUL).toLocalDate();
    }
}
