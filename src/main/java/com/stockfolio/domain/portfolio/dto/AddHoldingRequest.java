package com.stockfolio.domain.portfolio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddHoldingRequest {

    @NotBlank(message = "종목코드는 필수입니다.")
    private String stockCode;

    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    private Integer quantity;

    @NotNull(message = "매수 단가는 필수입니다.")
    @DecimalMin(value = "0.0001", message = "매수 단가는 0보다 커야 합니다.")
    private BigDecimal price;
}
