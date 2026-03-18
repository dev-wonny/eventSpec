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
        EventEntity event = eventQueryPort.findById(query.eventId())
                .orElseThrow(() -> BusinessException.from(EventCode.EVENT_NOT_FOUND));

        if (event.getEventType() != EventType.ATTENDANCE) {
            throw BusinessException.from(EventCode.EVENT_NOT_ATTENDANCE);
        }

        List<EventRoundEntity> rounds = eventRoundQueryPort.findAllByEventId(query.eventId());

        if (query.memberId() == null) {
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

        List<EventWinEntity> wins = eventWinQueryPort.findByEventIdAndMemberId(query.eventId(), query.memberId());

        Set<Long> attendedRoundIds = eventEntryQueryPort.findAttendedRoundIdsByEventIdAndMemberId(
                query.eventId(),
                query.memberId()
        );

        Map<Long, EventWinEntity> winByRoundId = wins.stream()
                .collect(Collectors.toMap(
                        EventWinEntity::getRoundId,
                        Function.identity(),
                        (first, second) -> first
                ));

        Map<Long, EventRoundPrizeEntity> eventRoundPrizeMap = eventRoundPrizeQueryPort.findByIds(extractRoundPrizeIds(wins));
        Map<Long, PrizeEntity> prizeMap = prizeQueryPort.findByIds(extractPrizeIds(eventRoundPrizeMap.values()));

        LocalDate today = LocalDate.now(AppTimeZones.ASIA_SEOUL);
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

        return EventDetailDto.of(
                event,
                roundDtos,
                AttendanceSummaryDto.of(attendedRoundIds.size(), roundDtos.size())
        );
    }

    private Set<Long> extractRoundPrizeIds(List<EventWinEntity> wins) {
        return wins.stream()
                .map(EventWinEntity::getEventRoundPrizeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<Long> extractPrizeIds(Collection<EventRoundPrizeEntity> eventRoundPrizes) {
        return eventRoundPrizes.stream()
                .map(EventRoundPrizeEntity::getPrizeId)
                .collect(Collectors.toSet());
    }

    private EventWinInfoDto buildWinInfo(
            EventWinEntity win,
            Map<Long, EventRoundPrizeEntity> eventRoundPrizeMap,
            Map<Long, PrizeEntity> prizeMap
    ) {
        if (win == null || win.getEventRoundPrizeId() == null) {
            return null;
        }

        EventRoundPrizeEntity eventRoundPrize = eventRoundPrizeMap.get(win.getEventRoundPrizeId());
        if (eventRoundPrize == null) {
            return null;
        }

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
        if (attendedRoundIds.contains(round.getId())) {
            return AttendanceStatus.ATTENDED;
        }

        LocalDate roundDate = resolveRoundDate(event, round);
        if (roundDate.isEqual(today)) {
            return AttendanceStatus.TODAY;
        }
        if (roundDate.isAfter(today)) {
            return AttendanceStatus.FUTURE;
        }
        return AttendanceStatus.MISSED;
    }

    private LocalDate resolveRoundDate(EventEntity event, EventRoundEntity round) {
        if (round.getRoundStartAt() != null) {
            return round.getRoundStartAt().atZone(AppTimeZones.ASIA_SEOUL).toLocalDate();
        }

        Instant baseStart = event.getStartAt().plusSeconds((long) (round.getRoundNo() - 1) * 24 * 60 * 60);
        return baseStart.atZone(AppTimeZones.ASIA_SEOUL).toLocalDate();
    }
}
