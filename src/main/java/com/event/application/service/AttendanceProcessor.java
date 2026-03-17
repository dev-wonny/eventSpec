package com.event.application.service;

import com.event.application.dto.attendance.AttendEventResult;
import com.event.application.dto.attendance.AttendanceRewardInfo;
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

