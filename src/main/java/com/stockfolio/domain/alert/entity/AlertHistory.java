package com.stockfolio.domain.alert.entity;

import com.stockfolio.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_histories",
        indexes = @Index(name = "idx_alert_hist_sent_at", columnList = "sent_at"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_alert_id", nullable = false)
    private PriceAlert priceAlert;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @Builder
    private AlertHistory(User user, PriceAlert priceAlert, String message,
                          Channel channel, Status status, String errorMessage) {
        this.user = user;
        this.priceAlert = priceAlert;
        this.message = message;
        this.channel = channel;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public enum Channel {
        EMAIL, SLACK
    }

    public enum Status {
        SUCCESS, FAILED
    }
}
