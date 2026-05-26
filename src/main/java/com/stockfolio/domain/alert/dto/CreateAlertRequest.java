package com.stockfolio.domain.alert.dto;

import com.stockfolio.domain.alert.entity.PriceAlert;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class CreateAlertRequest {

    @NotBlank(message = "종목코드는 필수입니다.")
    private String stockCode;

    @NotNull(message = "알림 유형은 필수입니다.")
    private PriceAlert.AlertType alertType;

    @NotNull(message = "기준가는 필수입니다.")
    @DecimalMin(value = "0.0001", message = "기준가는 0보다 커야 합니다.")
    private BigDecimal targetPrice;
}
