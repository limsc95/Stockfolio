package com.stockfolio.infra.rabbitmq;

import com.stockfolio.domain.alert.entity.AlertHistory;
import com.stockfolio.domain.alert.entity.PriceAlert;
import com.stockfolio.domain.alert.repository.AlertHistoryRepository;
import com.stockfolio.domain.alert.repository.PriceAlertRepository;
import com.stockfolio.domain.user.entity.User;
import com.stockfolio.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import static com.stockfolio.global.config.RabbitMQConfig.ALERT_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertMessageConsumer {

    private final JavaMailSender mailSender;
    private final AlertHistoryRepository alertHistoryRepository;
    private final PriceAlertRepository priceAlertRepository;
    private final UserRepository userRepository;

    @RabbitListener(queues = ALERT_QUEUE)
    public void consume(AlertMessage message) {
        log.info("[AlertConsumer] 메시지 수신 - alertId={}, userId={}, stock={}",
                message.alertId(), message.userId(), message.stockCode());

        PriceAlert alert = priceAlertRepository.findById(message.alertId()).orElse(null);
        User user = userRepository.findByIdAndDeletedAtIsNull(message.userId()).orElse(null);

        if (alert == null || user == null) {
            log.warn("[AlertConsumer] 알림 또는 유저 없음 - alertId={}", message.alertId());
            return;
        }

        sendEmail(message, alert, user);
    }

    // ── 이메일 발송 + 이력 저장 ───────────────────────────
    private void sendEmail(AlertMessage msg, PriceAlert alert, User user) {
        String subject = buildSubject(msg);
        String body = buildBody(msg);
        String errorMessage = null;
        AlertHistory.Status status;

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(user.getEmail());
            mail.setSubject(subject);
            mail.setText(body);
            mailSender.send(mail);
            status = AlertHistory.Status.SUCCESS;
            log.info("[AlertConsumer] 이메일 발송 성공 - to={}", user.getEmail());
        } catch (MailException e) {
            status = AlertHistory.Status.FAILED;
            errorMessage = e.getMessage();
            log.error("[AlertConsumer] 이메일 발송 실패 - to={}, error={}", user.getEmail(), e.getMessage());
        }

        alertHistoryRepository.save(
                AlertHistory.builder()
                        .user(user)
                        .priceAlert(alert)
                        .message(body)
                        .channel(AlertHistory.Channel.EMAIL)
                        .status(status)
                        .errorMessage(errorMessage)
                        .build()
        );
    }

    // ── 메시지 포맷 ───────────────────────────────────────
    private String buildSubject(AlertMessage msg) {
        String typeLabel = "TARGET_PRICE".equals(msg.alertType()) ? "목표가" : "손절가";
        return String.format("[StockFolio] %s(%s) %s 도달 알림",
                msg.stockName(), msg.stockCode(), typeLabel);
    }

    private String buildBody(AlertMessage msg) {
        String typeLabel = "TARGET_PRICE".equals(msg.alertType()) ? "목표가" : "손절가";
        String condition = "TARGET_PRICE".equals(msg.alertType()) ? "이상" : "이하";
        return String.format("""
                안녕하세요, %s님.

                설정하신 가격 알림이 발동되었습니다.

                ▶ 종목: %s (%s)
                ▶ 유형: %s
                ▶ 기준가: %s원 %s
                ▶ 현재가: %s원

                StockFolio에 접속하여 포트폴리오를 확인해 주세요.
                """,
                msg.userName(),
                msg.stockName(), msg.stockCode(),
                typeLabel,
                msg.targetPrice().toPlainString(), condition,
                msg.triggeredPrice().toPlainString()
        );
    }
}
