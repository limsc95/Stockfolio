package com.stockfolio.infra.rabbitmq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.stockfolio.global.config.RabbitMQConfig.ALERT_EXCHANGE;
import static com.stockfolio.global.config.RabbitMQConfig.ALERT_ROUTING_KEY;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(AlertMessage message) {
        rabbitTemplate.convertAndSend(ALERT_EXCHANGE, ALERT_ROUTING_KEY, message);
        log.info("[AlertPublisher] 메시지 발행 - alertId={}, stockCode={}, type={}",
                message.alertId(), message.stockCode(), message.alertType());
    }
}
