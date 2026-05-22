package com.stockfolio.domain.portfolio.dto;

import com.stockfolio.domain.portfolio.entity.Portfolio;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class PortfolioResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final List<HoldingResponse> holdings;

    // 집계 수치
    private final BigDecimal totalInvestment;     // 총 투자금액
    private final BigDecimal totalCurrentValue;   // 총 평가금액
    private final BigDecimal totalProfitLoss;     // 총 평가손익
    private final BigDecimal totalProfitLossRate; // 총 수익률(%)
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private PortfolioResponse(Portfolio portfolio, List<HoldingResponse> holdings) {
        this.id = portfolio.getId();
        this.name = portfolio.getName();
        this.description = portfolio.getDescription();
        this.holdings = holdings;
        this.createdAt = portfolio.getCreatedAt();
        this.updatedAt = portfolio.getUpdatedAt();

        this.totalInvestment = holdings.stream()
                .map(HoldingResponse::getInvestmentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalCurrentValue = holdings.stream()
                .map(HoldingResponse::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalProfitLoss = totalCurrentValue.subtract(totalInvestment);

        if (totalInvestment.compareTo(BigDecimal.ZERO) > 0) {
            this.totalProfitLossRate = totalProfitLoss
                    .divide(totalInvestment, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            this.totalProfitLossRate = BigDecimal.ZERO;
        }
    }

    public static PortfolioResponse of(Portfolio portfolio, List<HoldingResponse> holdings) {
        return new PortfolioResponse(portfolio, holdings);
    }
}
