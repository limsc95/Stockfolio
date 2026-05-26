package com.stockfolio.domain.alert.service;

import com.stockfolio.domain.alert.dto.AlertHistoryResponse;
import com.stockfolio.domain.alert.dto.AlertResponse;
import com.stockfolio.domain.alert.dto.CreateAlertRequest;
import com.stockfolio.domain.alert.entity.PriceAlert;
import com.stockfolio.domain.alert.repository.AlertHistoryRepository;
import com.stockfolio.domain.alert.repository.PriceAlertRepository;
import com.stockfolio.domain.stock.entity.Stock;
import com.stockfolio.domain.stock.repository.StockRepository;
import com.stockfolio.domain.stock.service.StockPriceService;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final PriceAlertRepository alertRepository;
    private final AlertHistoryRepository historyRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;
    private final StockPriceService stockPriceService;

    // ── 알림 설정 생성 ────────────────────────────────────
    @Transactional
    public AlertResponse create(Long userId, CreateAlertRequest request) {
        if (!request.hasValidInput()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Stock stock = stockRepository.findById(request.getStockCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));

        // ── 기준가 계산 ───────────────────────────────────
        BigDecimal referencePrice = null;
        BigDecimal targetPrice;

        if (request.getTargetPercent() != null) {
            // 비율 입력: 현재가를 조회하여 계산
            referencePrice = stockPriceService.getCurrentPrice(stock.getCode(), stock.getMarket());
            if (referencePrice.compareTo(BigDecimal.ZERO) == 0) {
                throw new BusinessException(ErrorCode.STOCK_PRICE_UNAVAILABLE);
            }

            // targetPrice = referencePrice × (1 + percent / 100)
            BigDecimal multiplier = BigDecimal.ONE
                    .add(request.getTargetPercent().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
            targetPrice = referencePrice.multiply(multiplier).setScale(0, RoundingMode.HALF_UP);

            log.info("[Alert] 비율 입력 - referencePrice={}, percent={}%, targetPrice={}",
                    referencePrice, request.getTargetPercent(), targetPrice);
        } else {
            targetPrice = request.getTargetPrice();
        }

        PriceAlert alert = PriceAlert.builder()
                .user(user)
                .stockCode(stock.getCode())
                .stockName(stock.getName())
                .alertType(request.getAlertType())
                .targetPrice(targetPrice)
                .referencePrice(referencePrice)
                .build();

        PriceAlert saved = alertRepository.save(alert);
        log.info("[Alert] 생성 - userId={}, stock={}, type={}, targetPrice={}, referencePrice={}",
                userId, stock.getCode(), request.getAlertType(), targetPrice, referencePrice);
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
