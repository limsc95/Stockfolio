package com.stockfolio.domain.portfolio.dto;

import com.stockfolio.domain.portfolio.entity.PortfolioHolding;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class HoldingResponse {

    private final Long id;
    private final String stockCode;
    private final String stockName;
    private final int quantity;
    private final BigDecimal averagePrice;
    private final BigDecimal currentPrice;
    private final BigDecimal investmentAmount;   // 평균가 × 수량
    private final BigDecimal currentValue;        // 현재가 × 수량
    private final BigDecimal profitLoss;          // 평가손익
    private final BigDecimal profitLossRate;      // 수익률(%)

    private HoldingResponse(PortfolioHolding holding, BigDecimal currentPrice) {
        this.id = holding.getId();
        this.stockCode = holding.getStockCode();
        this.stockName = holding.getStockName();
        this.quantity = holding.getQuantity();
        this.averagePrice = holding.getAveragePrice();
        this.currentPrice = currentPrice;

        BigDecimal qty = BigDecimal.valueOf(holding.getQuantity());
        this.investmentAmount = holding.getAveragePrice().multiply(qty);
        this.currentValue = currentPrice.multiply(qty);
        this.profitLoss = this.currentValue.subtract(this.investmentAmount);

        if (this.investmentAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.profitLossRate = this.profitLoss
                    .divide(this.investmentAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            this.profitLossRate = BigDecimal.ZERO;
        }
    }

    public static HoldingResponse of(PortfolioHolding holding, BigDecimal currentPrice) {
        return new HoldingResponse(holding, currentPrice);
    }
}
