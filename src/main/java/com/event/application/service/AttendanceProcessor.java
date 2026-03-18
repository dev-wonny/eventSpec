package com.event.application.service;

import com.event.application.dto.attendance.internal.AttendanceRewardInfo;
import com.event.application.dto.attendance.result.AttendEventResult;
import com.event.domain.entity.EventApplicantEntity;
import com.event.domain.entity.EventRoundEntity;

public interface AttendanceProcessor {

    AttendEventResult process(
            EventApplicantEntity applicant,
            EventRoundEntity round,
            Long memberId,
            AttendanceRewardInfo rewardInfo,
            int attendedDays,
            int totalDays
    );
}
