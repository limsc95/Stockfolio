package com.stockfolio.domain.alert.entity;

import com.stockfolio.domain.user.entity.User;
import com.stockfolio.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "price_alerts",
        indexes = @Index(name = "idx_alert_stock_triggered", columnList = "stock_code, is_triggered"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceAlert extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String stockCode;

    @Column(nullable = false, length = 100)
    private String stockName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertType alertType;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal targetPrice;

    @Column(nullable = false)
    private boolean isTriggered;

    @Column
    private LocalDateTime triggeredAt;

    @OneToMany(mappedBy = "priceAlert", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AlertHistory> alertHistories = new ArrayList<>();

    @Builder
    private PriceAlert(User user, String stockCode, String stockName,
                        AlertType alertType, BigDecimal targetPrice) {
        this.user = user;
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.alertType = alertType;
        this.targetPrice = targetPrice;
        this.isTriggered = false;
    }

    // ── 도메인 메서드 ──────────────────────────────────────
    public void trigger() {
        this.isTriggered = true;
        this.triggeredAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    public enum AlertType {
        TARGET_PRICE,   // 목표가 도달
        STOP_LOSS       // 손절가 도달
    }
}
