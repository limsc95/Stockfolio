package com.stockfolio.domain.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_histories",
        indexes = @Index(name = "idx_trade_traded_at", columnList = "traded_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_holding_id", nullable = false)
    private PortfolioHolding portfolioHolding;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TradeType tradeType;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDateTime tradedAt;

    @Builder
    private TradeHistory(PortfolioHolding portfolioHolding, TradeType tradeType,
                          int quantity, BigDecimal price) {
        this.portfolioHolding = portfolioHolding;
        this.tradeType = tradeType;
        this.quantity = quantity;
        this.price = price;
        this.tradedAt = LocalDateTime.now();
    }

    public enum TradeType {
        BUY, SELL
    }
}
