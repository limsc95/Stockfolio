package com.stockfolio.domain.portfolio.dto;

import com.stockfolio.domain.portfolio.entity.TradeHistory;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class TradeHistoryResponse {

    private final Long id;
    private final String tradeType;
    private final String stockCode;
    private final String stockName;
    private final int quantity;
    private final BigDecimal price;
    private final BigDecimal totalAmount;  // 수량 × 단가
    private final LocalDateTime tradedAt;

    private TradeHistoryResponse(TradeHistory trade) {
        this.id = trade.getId();
        this.tradeType = trade.getTradeType().name();
        this.stockCode = trade.getPortfolioHolding().getStockCode();
        this.stockName = trade.getPortfolioHolding().getStockName();
        this.quantity = trade.getQuantity();
        this.price = trade.getPrice();
        this.totalAmount = trade.getPrice().multiply(BigDecimal.valueOf(trade.getQuantity()));
        this.tradedAt = trade.getTradedAt();
    }

    public static TradeHistoryResponse from(TradeHistory trade) {
        return new TradeHistoryResponse(trade);
    }
}
