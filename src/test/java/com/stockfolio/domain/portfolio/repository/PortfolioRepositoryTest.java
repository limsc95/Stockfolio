package com.stockfolio.domain.portfolio.repository;

import com.stockfolio.domain.portfolio.entity.Portfolio;
import com.stockfolio.domain.portfolio.entity.PortfolioHolding;
import com.stockfolio.domain.user.entity.User;
import com.stockfolio.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * PortfolioRepository 통합 테스트
 *
 * 핵심 검증 포인트:
 *   1. findAllByUserId()  — 본인 포트폴리오만 반환되는지
 *   2. findByIdWithHoldings() — JPQL LEFT JOIN FETCH가 N+1을 피해 보유 종목을 한 번에 로드하는지
 */
@DataJpaTest
@DisplayName("PortfolioRepository 통합 테스트")
class PortfolioRepositoryTest {

    @Autowired PortfolioRepository       portfolioRepository;
    @Autowired PortfolioHoldingRepository holdingRepository;
    @Autowired UserRepository            userRepository;

    private User owner;   // 각 테스트에서 공통 사용할 소유자

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .email("owner@test.com")
                .password("encoded")
                .name("소유자")
                .role(User.Role.USER)
                .build());
    }

    // ── 헬퍼 ─────────────────────────────────────────────
    private Portfolio savePortfolio(String name) {
        return portfolioRepository.save(Portfolio.builder()
                .user(owner)
                .name(name)
                .description("테스트 포트폴리오")
                .build());
    }

    private void addHolding(Portfolio portfolio, String code, String name) {
        holdingRepository.save(PortfolioHolding.builder()
                .portfolio(portfolio)
                .stockCode(code)
                .stockName(name)
                .quantity(10)
                .averagePrice(new BigDecimal("70000"))
                .build());
    }

    // ════════════════════════════════════════════════════
    // findAllByUserId()
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("findAllByUserId()")
    class FindAllByUserIdTest {

        @Test
        @DisplayName("유저 ID로 본인의 포트폴리오 전체 목록을 반환해야 한다")
        void returns_all_portfolios_for_owner() {
            // given: 소유자 포트폴리오 2개
            savePortfolio("포트폴리오 A");
            savePortfolio("포트폴리오 B");

            // when
            List<Portfolio> result = portfolioRepository.findAllByUserId(owner.getId());

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(Portfolio::getName)
                    .containsExactlyInAnyOrder("포트폴리오 A", "포트폴리오 B");
        }

        @Test
        @DisplayName("포트폴리오가 없는 유저는 빈 목록을 반환해야 한다")
        void returns_empty_list_when_no_portfolios() {
            // given: 다른 유저 (포트폴리오 미생성)
            User other = userRepository.save(User.builder()
                    .email("other@test.com").password("pw").name("타인").role(User.Role.USER)
                    .build());

            // when
            List<Portfolio> result = portfolioRepository.findAllByUserId(other.getId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("타인의 포트폴리오는 포함되지 않아야 한다")
        void other_users_portfolios_not_included() {
            // given: 소유자 포트폴리오 1개, 타인 포트폴리오 1개
            savePortfolio("내 포트폴리오");
            User other = userRepository.save(User.builder()
                    .email("stranger@test.com").password("pw").name("타인").role(User.Role.USER)
                    .build());
            portfolioRepository.save(Portfolio.builder()
                    .user(other).name("타인의 포트폴리오").description("").build());

            // when
            List<Portfolio> result = portfolioRepository.findAllByUserId(owner.getId());

            // then: 소유자의 포트폴리오만
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("내 포트폴리오");
        }
    }

    // ════════════════════════════════════════════════════
    // findByIdWithHoldings()
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("findByIdWithHoldings()")
    class FindByIdWithHoldingsTest {

        @Test
        @DisplayName("보유 종목이 있는 포트폴리오를 한 번의 쿼리로 조회해야 한다")
        void returns_portfolio_with_holdings_in_one_query() {
            // given
            Portfolio portfolio = savePortfolio("종목 보유 포트폴리오");
            addHolding(portfolio, "005930", "삼성전자");
            addHolding(portfolio, "000660", "SK하이닉스");

            // when
            Optional<Portfolio> result = portfolioRepository.findByIdWithHoldings(portfolio.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getHoldings()).hasSize(2);
            assertThat(result.get().getHoldings())
                    .extracting(PortfolioHolding::getStockCode)
                    .containsExactlyInAnyOrder("005930", "000660");
        }

        @Test
        @DisplayName("보유 종목이 없는 포트폴리오도 정상 조회되어야 한다")
        void returns_portfolio_without_holdings() {
            // given
            Portfolio portfolio = savePortfolio("빈 포트폴리오");

            // when
            Optional<Portfolio> result = portfolioRepository.findByIdWithHoldings(portfolio.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getHoldings()).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 ID 조회 시 빈 Optional을 반환해야 한다")
        void returns_empty_for_unknown_id() {
            Optional<Portfolio> result = portfolioRepository.findByIdWithHoldings(999_999L);

            assertThat(result).isEmpty();
        }
    }

    // ════════════════════════════════════════════════════
    // PortfolioHoldingRepository.findByPortfolioIdAndStockCode()
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("PortfolioHoldingRepository.findByPortfolioIdAndStockCode()")
    class HoldingQueryTest {

        @Test
        @DisplayName("포트폴리오 ID와 종목 코드로 보유 종목을 조회해야 한다")
        void find_holding_by_portfolio_and_stock_code() {
            // given
            Portfolio portfolio = savePortfolio("홀딩 조회 포트폴리오");
            addHolding(portfolio, "005930", "삼성전자");

            // when
            Optional<PortfolioHolding> result =
                    holdingRepository.findByPortfolioIdAndStockCode(portfolio.getId(), "005930");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getStockName()).isEqualTo("삼성전자");
        }

        @Test
        @DisplayName("등록되지 않은 종목 코드 조회 시 빈 Optional을 반환해야 한다")
        void returns_empty_for_unregistered_stock() {
            Portfolio portfolio = savePortfolio("조회 실패 포트폴리오");

            Optional<PortfolioHolding> result =
                    holdingRepository.findByPortfolioIdAndStockCode(portfolio.getId(), "999999");

            assertThat(result).isEmpty();
        }
    }
}
