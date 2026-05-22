package com.stockfolio.infra.rabbitmq;

import java.math.BigDecimal;

/**
 * RabbitMQ를 통해 전달되는 알림 메시지 DTO
 * Record로 선언 → 직렬화/역직렬화 자동
 */
public record AlertMessage(
        Long alertId,
        Long userId,
        String userEmail,
        String userName,
        String stockCode,
        String stockName,
        String alertType,        // "TARGET_PRICE" | "STOP_LOSS"
        BigDecimal targetPrice,
        BigDecimal triggeredPrice
) {}
