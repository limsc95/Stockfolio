package com.stockfolio.domain.alert.service;

import com.stockfolio.domain.alert.dto.AlertHistoryResponse;
import com.stockfolio.domain.alert.dto.AlertResponse;
import com.stockfolio.domain.alert.dto.CreateAlertRequest;
import com.stockfolio.domain.alert.entity.PriceAlert;
import com.stockfolio.domain.alert.repository.AlertHistoryRepository;
import com.stockfolio.domain.alert.repository.PriceAlertRepository;
import com.stockfolio.domain.stock.entity.Stock;
import com.stockfolio.domain.stock.repository.StockRepository;
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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final PriceAlertRepository alertRepository;
    private final AlertHistoryRepository historyRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;

    // ── 알림 설정 생성 ────────────────────────────────────
    @Transactional
    public AlertResponse create(Long userId, CreateAlertRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Stock stock = stockRepository.findById(request.getStockCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));

        PriceAlert alert = PriceAlert.builder()
                .user(user)
                .stockCode(stock.getCode())
                .stockName(stock.getName())
                .alertType(request.getAlertType())
                .targetPrice(request.getTargetPrice())
                .build();

        PriceAlert saved = alertRepository.save(alert);
        log.info("[Alert] 생성 - userId={}, stock={}, type={}, targetPrice={}",
                userId, stock.getCode(), request.getAlertType(), request.getTargetPrice());
        return AlertResponse.from(saved);
    }

    // ── 알림 목록 조회 ────────────────────────────────────
    @Transactional(readOnly = true)
    public List<AlertResponse> getList(Long userId, Boolean isTriggered) {
        List<PriceAlert> alerts = (isTriggered != null)
                ? alertRepository.findAllByUserIdAndIsTriggeredOrderByCreatedAtDesc(userId, isTriggered)
                : alertRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        return alerts.stream().map(AlertResponse::from).toList();
    }

    // ── 알림 삭제 ─────────────────────────────────────────
    @Transactional
    public void delete(Long userId, Long alertId) {
        PriceAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALERT_NOT_FOUND));

        if (!alert.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.ALERT_ACCESS_DENIED);
        }

        alertRepository.delete(alert);
        log.info("[Alert] 삭제 - userId={}, alertId={}", userId, alertId);
    }

    // ── 발송 이력 조회 (내 것) ────────────────────────────
    @Transactional(readOnly = true)
    public PageResponse<AlertHistoryResponse> getMyHistories(Long userId, Pageable pageable) {
        return PageResponse.of(
                historyRepository.findAllByUserIdOrderBySentAtDesc(userId, pageable)
                        .map(AlertHistoryResponse::from)
        );
    }
}
