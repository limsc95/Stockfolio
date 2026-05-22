package com.stockfolio.domain.portfolio.entity;

import com.stockfolio.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "portfolio_holdings",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_holding_portfolio_stock",
                columnNames = {"portfolio_id", "stock_code"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioHolding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(nullable = false, length = 20)
    private String stockCode;

    @Column(nullable = false, length = 100)
    private String stockName;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal averagePrice;

    @OneToMany(mappedBy = "portfolioHolding", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TradeHistory> tradeHistories = new ArrayList<>();

    // ── 생성자 ────────────────────────────────────────────
    @Builder
    private PortfolioHolding(Portfolio portfolio, String stockCode, String stockName,
                              int quantity, BigDecimal averagePrice) {
        this.portfolio = portfolio;
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
    }

    // ── 도메인 메서드: 매수 (평균가 재계산) ─────────────────
    public void buy(int buyQuantity, BigDecimal buyPrice) {
        BigDecimal totalCost = averagePrice.multiply(BigDecimal.valueOf(quantity))
                .add(buyPrice.multiply(BigDecimal.valueOf(buyQuantity)));
        int newQuantity = this.quantity + buyQuantity;
        this.averagePrice = totalCost.divide(BigDecimal.valueOf(newQuantity), 4, RoundingMode.HALF_UP);
        this.quantity = newQuantity;
    }

    // ── 도메인 메서드: 매도 ───────────────────────────────
    public void sell(int sellQuantity) {
        if (this.quantity < sellQuantity) {
            throw new IllegalArgumentException("보유 수량보다 매도 수량이 많습니다.");
        }
        this.quantity -= sellQuantity;
    }

    public boolean isOwnedBy(Long portfolioId) {
        return this.portfolio.getId().equals(portfolioId);
    }
}
