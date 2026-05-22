package com.stockfolio.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardStatsResponse {

    private final long totalUsers;
    private final long activeUsers;        // isActive = true
    private final long totalPortfolios;
    private final long totalAlerts;        // 전체 알림 설정 수
    private final long triggeredToday;     // 오늘 발동된 알림 수
    private final long alertsSentToday;    // 오늘 발송된 이메일 수
    private final long alertFailedToday;   // 오늘 실패한 발송 수
}
