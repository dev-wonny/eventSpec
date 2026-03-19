package com.event.application.service;

import com.event.application.dto.attendance.internal.AttendanceRewardInfo;
import com.event.application.dto.attendance.result.AttendEventResult;
import com.event.domain.entity.EventApplicantEntity;
import com.event.domain.entity.EventRoundEntity;

/**
 * 출석 로컬 저장 결과를 최종 유스케이스 결과로 바꾸는 처리 전략.
 *
 * applicant/round/보상 정보가 준비된 뒤,
 * 구현체가 entry/win 저장과 AttendEventResult 조립을 맡는다.
 */
public interface AttendanceProcessor {

    /**
     * 출석 트랜잭션에서 준비된 데이터로 entry/win 저장과 최종 결과 조립을 수행한다.
     * 구현체는 보상 유무에 따라 저장 대상과 응답 구조를 다르게 만들 수 있다.
     */
    AttendEventResult process(
            EventApplicantEntity applicant,
            EventRoundEntity round,
            Long memberId,
            AttendanceRewardInfo rewardInfo,
            long attendedDays,
            long totalDays
    );
}
