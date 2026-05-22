package com.stockfolio.global.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ── Exchange / Queue / Binding 이름 상수 ────────────
    public static final String ALERT_EXCHANGE      = "stockfolio.alert.exchange";
    public static final String ALERT_QUEUE         = "stockfolio.alert.queue";
    public static final String ALERT_ROUTING_KEY   = "alert.price";

    // Dead Letter (재시도 실패 처리)
    public static final String ALERT_DLQ           = "stockfolio.alert.dlq";
    public static final String ALERT_DLX           = "stockfolio.alert.dlx";

    // ── Exchange ─────────────────────────────────────────
    @Bean
    public DirectExchange alertExchange() {
        return new DirectExchange(ALERT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(ALERT_DLX, true, false);
    }

    // ── Queue ─────────────────────────────────────────────
    @Bean
    public Queue alertQueue() {
        return QueueBuilder.durable(ALERT_QUEUE)
                .withArgument("x-dead-letter-exchange", ALERT_DLX)
                .withArgument("x-dead-letter-routing-key", ALERT_DLQ)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(ALERT_DLQ).build();
    }

    // ── Binding ───────────────────────────────────────────
    @Bean
    public Binding alertBinding(Queue alertQueue, DirectExchange alertExchange) {
        return BindingBuilder.bind(alertQueue).to(alertExchange).with(ALERT_ROUTING_KEY);
    }

    // ── Message Converter (JSON) ──────────────────────────
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
