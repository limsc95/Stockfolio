package com.stockfolio.domain.portfolio.service;

import com.stockfolio.domain.portfolio.dto.*;
import com.stockfolio.domain.portfolio.entity.Portfolio;
import com.stockfolio.domain.portfolio.entity.PortfolioHolding;
import com.stockfolio.domain.portfolio.entity.TradeHistory;
import com.stockfolio.domain.portfolio.repository.PortfolioHoldingRepository;
import com.stockfolio.domain.portfolio.repository.PortfolioRepository;
import com.stockfolio.domain.portfolio.repository.TradeHistoryRepository;
import com.stockfolio.domain.stock.service.StockPriceService;
import com.stockfolio.domain.user.entity.User;
import com.stockfolio.domain.user.repository.UserRepository;
import com.stockfolio.global.exception.BusinessException;
import com.stockfolio.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioService 단위 테스트")
class PortfolioServiceTest {

    @Mock PortfolioRepository        portfolioRepository;
    @Mock PortfolioHoldingRepository holdingRepository;
    @Mock TradeHistoryRepository     tradeHistoryRepository;
    @Mock UserRepository             userRepository;
    @Mock StockPriceService          stockPriceService;

    @InjectMocks PortfolioService portfolioService;

    // ── 픽스처 헬퍼 ───────────────────────────────────────
    private User user(Long id) {
        User u = User.builder()
                .email("user@test.com").password("pw").name("테스터").role(User.Role.USER)
                .build();
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private Portfolio portfolio(Long portfolioId, User owner) {
        Portfolio p = Portfolio.builder()
                .user(owner).name("내 포트폴리오").description("설명")
                .build();
        ReflectionTestUtils.setField(p, "id", portfolioId);
        return p;
    }

    private PortfolioHolding holding(Long holdingId, Portfolio portfolio, int quantity, String avgPrice) {
        PortfolioHolding h = PortfolioHolding.builder()
                .portfolio(portfolio)
                .stockCode("005930").stockName("삼성전자")
                .quantity(quantity)
                .averagePrice(new BigDecimal(avgPrice))
                .build();
        ReflectionTestUtils.setField(h, "id", holdingId);
        return h;
    }

    // ════════════════════════════════════════════════════
    // create()
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("포트폴리오 생성(create)")
    class CreateTest {

        @Test
        @DisplayName("정상 요청 시 저장 후 PortfolioResponse를 반환해야 한다")
        void create_success() {
            // given
            User owner = user(1L);
            Portfolio saved = portfolio(10L, owner);

            given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(owner));
            given(portfolioRepository.save(any(Portfolio.class))).willReturn(saved);

            // when
            PortfolioResponse result = portfolioService.create(
                    1L, new CreatePortfolioRequest("내 포트폴리오", "설명"));

            // then
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getName()).isEqualTo("내 포트폴리오");
            then(portfolioRepository).should().save(any(Portfolio.class));
        }

        @Test
        @DisplayName("존재하지 않는 유저면 USER_NOT_FOUND 예외가 발생해야 한다")
        void create_userNotFound_throwsException() {
            // given
            given(userRepository.findByIdAndDeletedAtIsNull(99L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    portfolioService.create(99L, new CreatePortfolioRequest("P", null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    // ════════════════════════════════════════════════════
    // delete()
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("포트폴리오 삭제(delete)")
    class DeleteTest {

        @Test
        @DisplayName("본인 포트폴리오 삭제 시 delete가 호출되어야 한다")
        void delete_owner_success() {
            // given
            User owner = user(1L);
            Portfolio p = portfolio(10L, owner);
            given(portfolioRepository.findById(10L)).willReturn(Optional.of(p));

            // when
            portfolioService.delete(1L, 10L);

            // then
            then(portfolioRepository).should().delete(p);
        }

        @Test
        @DisplayName("타인 포트폴리오 삭제 시 PORTFOLIO_ACCESS_DENIED 예외가 발생해야 한다")
        void delete_notOwner_throwsAccessDenied() {
            // given: userId=2 가 userId=1 의 포트폴리오 삭제 시도
            User owner = user(1L);
            Portfolio p = portfolio(10L, owner);
            given(portfolioRepository.findById(10L)).willReturn(Optional.of(p));

            // when & then
            assertThatThrownBy(() -> portfolioService.delete(2L, 10L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.PORTFOLIO_ACCESS_DENIED);

            then(portfolioRepository).should(never()).delete(any());
        }
    }

    // ════════════════════════════════════════════════════
    // addHolding() — 매수
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("종목 추가(addHolding)")
    class AddHoldingTest {

        @Test
        @DisplayName("신규 종목 추가 시 holding이 저장되고 거래 내역이 기록되어야 한다")
        void addHolding_newStock_savesHoldingAndTrade() {
            // given
            User owner = user(1L);
            Portfolio p = portfolio(10L, owner);
            PortfolioHolding savedHolding = holding(100L, p, 5, "70000");

            given(portfolioRepository.findById(10L)).willReturn(Optional.of(p));
            given(holdingRepository.findByPortfolioIdAndStockCode(10L, "005930"))
                    .willReturn(Optional.empty());
            given(holdingRepository.save(any(PortfolioHolding.class))).willReturn(savedHolding);
            given(tradeHistoryRepository.save(any(TradeHistory.class)))
                    .willReturn(mock(TradeHistory.class));
            given(stockPriceService.getCurrentPrice("005930"))
                    .willReturn(new BigDecimal("72000"));

            AddHoldingRequest request = new AddHoldingRequest("005930", 5, new BigDecimal("70000"));

            // when
            HoldingResponse result = portfolioService.addHolding(1L, 10L, request);

            // then
            assertThat(result.getStockCode()).isEqualTo("005930");
            then(holdingRepository).should().save(any(PortfolioHolding.class));
            then(tradeHistoryRepository).should().save(any(TradeHistory.class));
        }

        @Test
        @DisplayName("이미 보유한 종목 추가 시 기존 holding을 업데이트하고 거래 내역을 기록해야 한다")
        void addHolding_existingStock_updatesHoldingAndTrade() {
            // given
            User owner = user(1L);
            Portfolio p = portfolio(10L, owner);
            PortfolioHolding existing = holding(100L, p, 10, "50000");

            given(portfolioRepository.findById(10L)).willReturn(Optional.of(p));
            given(holdingRepository.findByPortfolioIdAndStockCode(10L, "005930"))
                    .willReturn(Optional.of(existing));
            given(tradeHistoryRepository.save(any(TradeHistory.class)))
                    .willReturn(mock(TradeHistory.class));
            given(stockPriceService.getCurrentPrice("005930"))
                    .willReturn(new BigDecimal("55000"));

            AddHoldingRequest request = new AddHoldingRequest("005930", 10, new BigDecimal("60000"));

            // when
            portfolioService.addHolding(1L, 10L, request);

            // then: 평균가 재계산 확인 (50000*10 + 60000*10) / 20 = 55000
            assertThat(existing.getQuantity()).isEqualTo(20);
            assertThat(existing.getAveragePrice())
                    .isEqualByComparingTo(new BigDecimal("55000.0000"));
            // 신규 save 는 호출되면 안 됨
            then(holdingRepository).should(never()).save(any());
        }
    }

    // ════════════════════════════════════════════════════
    // addTrade() — 매도
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("거래 기록 추가(addTrade) — 매도")
    class AddTradeTest {

        @Test
        @DisplayName("매도 수량이 보유량 초과 시 INSUFFICIENT_QUANTITY 예외가 발생해야 한다")
        void addTrade_sell_insufficientQuantity_throwsException() {
            // given
            User owner = user(1L);
            Portfolio p = portfolio(10L, owner);
            PortfolioHolding h = holding(100L, p, 5, "50000"); // 5주만 보유

            given(portfolioRepository.findById(10L)).willReturn(Optional.of(p));
            given(holdingRepository.findById(100L)).willReturn(Optional.of(h));

            TradeRequest request = new TradeRequest(
                    TradeHistory.TradeType.SELL, 10, new BigDecimal("55000")); // 10주 매도 시도

            // when & then
            assertThatThrownBy(() -> portfolioService.addTrade(1L, 10L, 100L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INSUFFICIENT_QUANTITY);
        }

        @Test
        @DisplayName("정상 매도 시 수량이 차감되고 거래 내역이 저장되어야 한다")
        void addTrade_sell_success() {
            // given
            User owner = user(1L);
            Portfolio p = portfolio(10L, owner);
            PortfolioHolding h = holding(100L, p, 10, "50000");

            given(portfolioRepository.findById(10L)).willReturn(Optional.of(p));
            given(holdingRepository.findById(100L)).willReturn(Optional.of(h));
            // save()에 전달된 TradeHistory를 그대로 반환 → TradeHistoryResponse.from() NPE 방지
            given(tradeHistoryRepository.save(any(TradeHistory.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            TradeRequest request = new TradeRequest(
                    TradeHistory.TradeType.SELL, 3, new BigDecimal("55000"));

            // when
            portfolioService.addTrade(1L, 10L, 100L, request);

            // then
            assertThat(h.getQuantity()).isEqualTo(7);
            then(tradeHistoryRepository).should().save(any(TradeHistory.class));
        }
    }
}
