package com.stockfolio.domain.admin.service;

import com.stockfolio.domain.admin.dto.AdminUserResponse;
import com.stockfolio.domain.admin.dto.DashboardStatsResponse;
import com.stockfolio.domain.alert.dto.AlertHistoryResponse;
import com.stockfolio.domain.alert.entity.AlertHistory;
import com.stockfolio.domain.alert.repository.AlertHistoryRepository;
import com.stockfolio.domain.alert.repository.PriceAlertRepository;
import com.stockfolio.domain.portfolio.repository.PortfolioRepository;
import com.stockfolio.domain.user.entity.User;
import com.stockfolio.domain.user.repository.UserRepository;
import com.stockfolio.global.common.PageResponse;
import com.stockfolio.global.exception.BusinessException;
import com.stockfolio.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final PriceAlertRepository alertRepository;
    private final AlertHistoryRepository historyRepository;

    // ── 대시보드 통계 ─────────────────────────────────────
    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        long totalUsers      = userRepository.count();
        long activeUsers     = userRepository.countByIsActiveTrue();
        long totalPortfolios = portfolioRepository.count();
        long totalAlerts     = alertRepository.count();
        long triggeredToday  = alertRepository.countTriggeredToday();
        long alertsSentToday = historyRepository.countSentToday();
        long alertFailed     = historyRepository.countFailedToday();

        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalPortfolios(totalPortfolios)
                .totalAlerts(totalAlerts)
                .triggeredToday(triggeredToday)
                .alertsSentToday(alertsSentToday)
                .alertFailedToday(alertFailed)
                .build();
    }

    // ── 유저 목록 조회 ────────────────────────────────────
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getUsers(String query, Pageable pageable) {
        var page = (query != null && !query.isBlank())
                ? userRepository.searchByEmailOrName(query, pageable)
                : userRepository.findAll(pageable);
        return PageResponse.of(page.map(AdminUserResponse::from));
    }

    // ── 유저 비활성화 ─────────────────────────────────────
    @Transactional
    public void deactivateUser(Long adminId, Long targetUserId) {
        if (adminId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.deactivate();
        log.info("[Admin] 유저 비활성화 - adminId={}, targetUserId={}", adminId, targetUserId);
    }

    // ── 알림 발송 이력 전체 ───────────────────────────────
    @Transactional(readOnly = true)
    public PageResponse<AlertHistoryResponse> getAlertHistories(AlertHistory.Status status,
                                                                 Pageable pageable) {
        var page = (status != null)
                ? historyRepository.findAllByStatusOrderBySentAtDesc(status, pageable)
                : historyRepository.findAll(pageable);
        return PageResponse.of(page.map(AlertHistoryResponse::from));
    }
}
