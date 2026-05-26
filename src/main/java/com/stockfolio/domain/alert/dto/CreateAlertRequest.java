package com.stockfolio.domain.alert.dto;

import com.stockfolio.domain.alert.entity.PriceAlert;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 알림 생성 요청 DTO
 *
 * 기준가 입력 방식 (둘 중 하나만 입력):
 *   - targetPrice  : 절대 금액 (예: 75000)
 *   - targetPercent: 현재가 기준 등락률 % (예: +10.0 이면 현재가 대비 10% 상승 시)
 *                    양수 = 상승, 음수 = 하락
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateAlertRequest {

    @NotBlank(message = "종목코드는 필수입니다.")
    private String stockCode;

    @NotNull(message = "알림 유형은 필수입니다.")
    private PriceAlert.AlertType alertType;

    /** 절대 금액 입력 (targetPercent 미입력 시 필수) */
    private BigDecimal targetPrice;

    /**
     * 현재가 기준 비율 입력 (선택)
     * 예) +10.0 → 현재가의 110%가 targetPrice
     *     -5.0  → 현재가의 95%가 targetPrice
     */
    private BigDecimal targetPercent;

    /**
     * targetPrice / targetPercent 중 하나는 반드시 입력돼야 함
     */
    public boolean hasValidInput() {
        return targetPrice != null || targetPercent != null;
    }
}
