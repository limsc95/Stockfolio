package com.stockfolio.domain.alert.dto;

import com.stockfolio.domain.alert.entity.PriceAlert;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class AlertResponse {

    private final Long id;
    private final String stockCode;
    private final String stockName;
    private final String alertType;
    private final BigDecimal targetPrice;
    private final BigDecimal referencePrice;   // 설정 당시 현재가 (비율 입력 시 표시용)
    private final boolean isTriggered;
    private final LocalDateTime createdAt;
    private final LocalDateTime triggeredAt;

    private AlertResponse(PriceAlert alert) {
        this.id = alert.getId();
        this.stockCode = alert.getStockCode();
        this.stockName = alert.getStockName();
        this.alertType = alert.getAlertType().name();
        this.targetPrice = alert.getTargetPrice();
        this.referencePrice = alert.getReferencePrice();
        this.isTriggered = alert.isTriggered();
        this.createdAt = alert.getCreatedAt();
        this.triggeredAt = alert.getTriggeredAt();
    }

    public static AlertResponse from(PriceAlert alert) {
        return new AlertResponse(alert);
    }
}
