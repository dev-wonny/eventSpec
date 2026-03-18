package com.event.application.service;

import com.event.application.dto.attendance.internal.AttendanceRewardInfo;
import com.event.application.dto.attendance.result.AttendEventResult;
import com.event.application.dto.attendance.result.AttendanceSummaryDto;
import com.event.application.dto.attendance.result.AttendanceWinDto;
import com.event.application.port.output.EventEntryCommandPort;
import com.event.application.port.output.EventWinCommandPort;
import com.event.domain.entity.EventApplicantEntity;
import com.event.domain.entity.EventEntryEntity;
import com.event.domain.entity.EventRoundEntity;
import com.event.domain.entity.EventWinEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 포인트형 출석 보상 처리 구현체.
 *
 * 출석 entry는 항상 저장하고,
 * 보상 매핑이 있는 경우에만 win을 추가 저장한 뒤 당첨 정보를 결과에 포함한다.
 */
@Component
@RequiredArgsConstructor
public class PointAttendanceProcessor implements AttendanceProcessor {

    private final EventEntryCommandPort eventEntryCommandPort;
    private final EventWinCommandPort eventWinCommandPort;

    @Override
    public AttendEventResult process(
            EventApplicantEntity applicant,
            EventRoundEntity round,
            Long memberId,
            AttendanceRewardInfo rewardInfo,
            int attendedDays,
            int totalDays
    ) {
        // 생성자/수정자 기록은 현재 요청 회원을 그대로 사용한다.
        Long actor = memberId;

        // 1. 출석 자체를 의미하는 entry를 먼저 저장한다.
        EventEntryEntity eventEntry = EventEntryEntity.create(
                applicant.getId(),
                applicant.getEventId(),
                memberId,
                rewardInfo != null ? rewardInfo.eventRoundPrizeId() : null,
                rewardInfo != null,
                actor
        );
        EventEntryEntity savedEntry = eventEntryCommandPort.save(eventEntry);

        if (rewardInfo == null) {
            // 보상이 없어도 entry는 남기고, 응답은 미당첨 출석 결과로 조립한다.
            // 2-1. 보상 매핑이 없으면 무보상 출석 결과를 바로 반환한다.
            return AttendEventResult.of(
                    savedEntry.getId(),
                    savedEntry.getAppliedAt(),
                    round.getRoundNo(),
                    Boolean.FALSE,
                    null,
                    AttendanceSummaryDto.of(attendedDays, totalDays)
            );
        }

        // 보상이 있는 회차만 win 레코드를 추가로 남긴다.
        // 2-2. 보상 매핑이 있으면 win을 추가 저장하고 당첨 정보를 결과에 포함한다.
        EventWinEntity savedWin = eventWinCommandPort.save(EventWinEntity.create(
                savedEntry.getId(),
                round.getId(),
                applicant.getEventId(),
                memberId,
                rewardInfo.eventRoundPrizeId(),
                actor
        ));

        return AttendEventResult.of(
                savedEntry.getId(),
                savedEntry.getAppliedAt(),
                round.getRoundNo(),
                Boolean.TRUE,
                AttendanceWinDto.of(
                        savedWin.getId(),
                        rewardInfo.prizeName(),
                        rewardInfo.rewardType(),
                        rewardInfo.pointAmount(),
                        null
                ),
                AttendanceSummaryDto.of(attendedDays, totalDays)
        );
    }
}
