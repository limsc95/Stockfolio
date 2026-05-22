package com.stockfolio.domain.portfolio.dto;

import com.stockfolio.domain.portfolio.entity.Portfolio;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class PortfolioSummaryResponse {

    private final Long id;
    private final String name;
    private final int holdingCount;
    private final BigDecimal totalProfitLossRate;  // 현재가 기반 수익률
    private final LocalDateTime updatedAt;

    private PortfolioSummaryResponse(Portfolio portfolio, BigDecimal totalProfitLossRate) {
        this.id = portfolio.getId();
        this.name = portfolio.getName();
        this.holdingCount = portfolio.getHoldings().size();
        this.totalProfitLossRate = totalProfitLossRate;
        this.updatedAt = portfolio.getUpdatedAt();
    }

    public static PortfolioSummaryResponse of(Portfolio portfolio, BigDecimal totalProfitLossRate) {
        return new PortfolioSummaryResponse(portfolio, totalProfitLossRate);
    }
}
