package com.stockfolio.domain.portfolio.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * PortfolioHolding 도메인 단위 테스트
 *
 * 외부 의존성 없이 순수 도메인 로직만 검증합니다.
 *   - 매수 시 가중평균가 재계산
 *   - 매도 시 수량 차감
 *   - 매도 수량 초과 예외
 */
@DisplayName("PortfolioHolding 도메인 테스트")
class PortfolioHoldingTest {

    // ── 테스트용 Holding 생성 헬퍼 ────────────────────────
    private PortfolioHolding holdingOf(int quantity, String averagePrice) {
        return PortfolioHolding.builder()
                .portfolio(null)          // 도메인 로직 테스트라 portfolio 불필요
                .stockCode("005930")
                .stockName("삼성전자")
                .quantity(quantity)
                .averagePrice(new BigDecimal(averagePrice))
                .build();
    }

    // ════════════════════════════════════════════════════
    // buy()
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("매수(buy)")
    class BuyTest {

        @Test
        @DisplayName("신규 매수 후 평균가가 매수가와 동일해야 한다")
        void buy_firstPurchase_averagePriceEqualsToBuyPrice() {
            // given
            PortfolioHolding holding = holdingOf(10, "50000");

            // when: 같은 가격으로 추가 매수
            holding.buy(10, new BigDecimal("50000"));

            // then
            assertThat(holding.getQuantity()).isEqualTo(20);
            assertThat(holding.getAveragePrice())
                    .isEqualByComparingTo(new BigDecimal("50000.0000"));
        }

        @Test
        @DisplayName("다른 가격으로 추가 매수 시 가중평균가로 재계산되어야 한다")
        void buy_additionalPurchase_recalculatesWeightedAverage() {
            // given: 10주 @ 50,000 보유
            PortfolioHolding holding = holdingOf(10, "50000");

            // when: 10주 @ 70,000 추가 매수
            holding.buy(10, new BigDecimal("70000"));

            // then: (50000*10 + 70000*10) / 20 = 60000
            assertThat(holding.getQuantity()).isEqualTo(20);
            assertThat(holding.getAveragePrice())
                    .isEqualByComparingTo(new BigDecimal("60000.0000"));
        }

        @Test
        @DisplayName("불균등 수량 매수 시 가중평균가가 정확히 계산되어야 한다")
        void buy_unequalQuantity_correctWeightedAverage() {
            // given: 3주 @ 60,000
            PortfolioHolding holding = holdingOf(3, "60000");

            // when: 7주 @ 40,000 추가 매수
            holding.buy(7, new BigDecimal("40000"));

            // then: (60000*3 + 40000*7) / 10 = (180000 + 280000) / 10 = 46000
            assertThat(holding.getQuantity()).isEqualTo(10);
            assertThat(holding.getAveragePrice())
                    .isEqualByComparingTo(new BigDecimal("46000.0000"));
        }
    }

    // ════════════════════════════════════════════════════
    // sell()
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("매도(sell)")
    class SellTest {

        @Test
        @DisplayName("매도 후 수량이 차감되어야 한다")
        void sell_decreasesQuantity() {
            // given
            PortfolioHolding holding = holdingOf(10, "50000");

            // when
            holding.sell(3);

            // then
            assertThat(holding.getQuantity()).isEqualTo(7);
        }

        @Test
        @DisplayName("매도 후 평균가는 변하지 않아야 한다")
        void sell_doesNotChangeAveragePrice() {
            // given
            PortfolioHolding holding = holdingOf(10, "50000");
            BigDecimal originalAvgPrice = holding.getAveragePrice();

            // when
            holding.sell(5);

            // then
            assertThat(holding.getAveragePrice())
                    .isEqualByComparingTo(originalAvgPrice);
        }

        @Test
        @DisplayName("전량 매도 시 수량이 0이 되어야 한다")
        void sell_allQuantity_quantityBecomesZero() {
            // given
            PortfolioHolding holding = holdingOf(10, "50000");

            // when
            holding.sell(10);

            // then
            assertThat(holding.getQuantity()).isZero();
        }

        @Test
        @DisplayName("보유 수량 초과 매도 시 예외가 발생해야 한다")
        void sell_exceedsQuantity_throwsException() {
            // given
            PortfolioHolding holding = holdingOf(5, "50000");

            // when & then
            assertThatThrownBy(() -> holding.sell(6))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("보유 수량");
        }
    }
}
