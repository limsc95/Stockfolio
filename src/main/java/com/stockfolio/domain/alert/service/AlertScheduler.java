package com.stockfolio.domain.alert.service;

import com.stockfolio.domain.alert.entity.PriceAlert;
import com.stockfolio.domain.alert.repository.PriceAlertRepository;
import com.stockfolio.domain.stock.service.StockPriceService;
import com.stockfolio.infra.rabbitmq.AlertMessage;
import com.stockfolio.infra.rabbitmq.AlertMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 가격 알림 스케줄러
 *
 * 30초마다 미발동 알림 전체를 조회한 뒤,
 * 종목코드 기준으로 그룹핑해서 현재가를 한 번씩만 조회.
 * 조건 충족 시 → PriceAlert.trigger() + RabbitMQ 발행.
 *
 * 조건:
 *   TARGET_PRICE : 현재가 >= 기준가  (목표가 도달)
 *   STOP_LOSS    : 현재가 <= 기준가  (손절가 도달)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertScheduler {

    private final PriceAlertRepository alertRepository;
    private final StockPriceService stockPriceService;
    private final AlertMessagePublisher publisher;

    @Scheduled(fixedDelay = 30_000)   // 30초마다 실행
    @Transactional
    public void checkAlerts() {
        List<PriceAlert> untriggered = alertRepository.findAllUntriggeredWithUser();
        if (untriggered.isEmpty()) return;

        log.debug("[AlertScheduler] 체크 시작 - 미발동 알림 {}건", untriggered.size());

        // 종목코드별 그룹핑 → 현재가 중복 조회 방지
        Map<String, List<PriceAlert>> byStock = untriggered.stream()
                .collect(Collectors.groupingBy(PriceAlert::getStockCode));

        byStock.forEach((stockCode, alerts) -> {
            BigDecimal currentPrice = stockPriceService.getCurrentPrice(stockCode);
            if (currentPrice.compareTo(BigDecimal.ZERO) == 0) {
                log.debug("[AlertScheduler] 현재가 미확인, 건너뜀 - stockCode={}", stockCode);
                return;
            }

            for (PriceAlert alert : alerts) {
                if (isConditionMet(alert, currentPrice)) {
                    alert.trigger();
                    publisher.publish(toMessage(alert, currentPrice));
                    log.info("[AlertScheduler] 알림 발동 - alertId={}, stock={}, type={}, target={}, current={}",
                            alert.getId(), stockCode, alert.getAlertType(),
                            alert.getTargetPrice(), currentPrice);
                }
            }
        });
    }

    // ── 조건 판단 ─────────────────────────────────────────
    private boolean isConditionMet(PriceAlert alert, BigDecimal currentPrice) {
        return switch (alert.getAlertType()) {
            case TARGET_PRICE -> currentPrice.compareTo(alert.getTargetPrice()) >= 0;
            case STOP_LOSS    -> currentPrice.compareTo(alert.getTargetPrice()) <= 0;
        };
    }

    // ── AlertMessage 생성 ─────────────────────────────────
    private AlertMessage toMessage(PriceAlert alert, BigDecimal currentPrice) {
        return new AlertMessage(
                alert.getId(),
                alert.getUser().getId(),
                alert.getUser().getEmail(),
                alert.getUser().getName(),
                alert.getStockCode(),
                alert.getStockName(),
                alert.getAlertType().name(),
                alert.getTargetPrice(),
                currentPrice
        );
    }
}
