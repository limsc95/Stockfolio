package com.stockfolio.domain.alert.dto;

import com.stockfolio.domain.alert.entity.AlertHistory;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AlertHistoryResponse {

    private final Long id;
    private final String stockCode;
    private final String stockName;
    private final String alertType;
    private final String message;
    private final String channel;
    private final String status;
    private final String errorMessage;
    private final LocalDateTime sentAt;

    private AlertHistoryResponse(AlertHistory history) {
        this.id = history.getId();
        this.stockCode = history.getPriceAlert().getStockCode();
        this.stockName = history.getPriceAlert().getStockName();
        this.alertType = history.getPriceAlert().getAlertType().name();
        this.message = history.getMessage();
        this.channel = history.getChannel().name();
        this.status = history.getStatus().name();
        this.errorMessage = history.getErrorMessage();
        this.sentAt = history.getSentAt();
    }

    public static AlertHistoryResponse from(AlertHistory history) {
        return new AlertHistoryResponse(history);
    }
}
