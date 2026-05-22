package com.stockfolio.domain.portfolio.dto;

import com.stockfolio.domain.portfolio.entity.TradeHistory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class TradeRequest {

    @NotNull(message = "거래 유형은 필수입니다.")
    private TradeHistory.TradeType tradeType;

    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    private Integer quantity;

    @NotNull(message = "거래 단가는 필수입니다.")
    @DecimalMin(value = "0.0001", message = "거래 단가는 0보다 커야 합니다.")
    private BigDecimal price;
}
