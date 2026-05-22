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
import com.stockfolio.global.common.PageResponse;
import com.stockfolio.global.exception.BusinessException;
import com.stockfolio.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioHoldingRepository holdingRepository;
    private final TradeHistoryRepository tradeHistoryRepository;
    private final UserRepository userRepository;
    private final StockPriceService stockPriceService;

    // ── 포트폴리오 생성 ───────────────────────────────────
    @Transactional
    public PortfolioResponse create(Long userId, CreatePortfolioRequest request) {
        User user = findUser(userId);
        Portfolio portfolio = Portfolio.builder()
                .user(user)
                .name(request.getName())
                .description(request.getDescription())
                .build();
        Portfolio saved = portfolioRepository.save(portfolio);
        log.info("[Portfolio] 생성 - userId={}, portfolioId={}", userId, saved.getId());
        return PortfolioResponse.of(saved, List.of());
    }

    // ── 포트폴리오 목록 ───────────────────────────────────
    @Transactional(readOnly = true)
    public List<PortfolioSummaryResponse> getList(Long userId) {
        return portfolioRepository.findAllByUserId(userId).stream()
                .map(p -> {
                    BigDecimal rate = calcTotalProfitLossRate(p);
                    return PortfolioSummaryResponse.of(p, rate);
                })
                .toList();
    }

    // ── 포트폴리오 상세 (현재가 기반 수익률) ─────────────
    @Transactional(readOnly = true)
    public PortfolioResponse getDetail(Long userId, Long portfolioId) {
        Portfolio portfolio = findPortfolioWithHoldings(portfolioId);
        checkOwnership(portfolio, userId);

        List<HoldingResponse> holdings = portfolio.getHoldings().stream()
                .map(h -> HoldingResponse.of(h, stockPriceService.getCurrentPrice(h.getStockCode())))
                .toList();

        return PortfolioResponse.of(portfolio, holdings);
    }

    // ── 포트폴리오 수정 ───────────────────────────────────
    @Transactional
    public PortfolioResponse update(Long userId, Long portfolioId, UpdatePortfolioRequest request) {
        Portfolio portfolio = findPortfolio(portfolioId);
        checkOwnership(portfolio, userId);
        portfolio.update(request.getName(), request.getDescription());

        List<HoldingResponse> holdings = portfolio.getHoldings().stream()
                .map(h -> HoldingResponse.of(h, stockPriceService.getCurrentPrice(h.getStockCode())))
                .toList();
        return PortfolioResponse.of(portfolio, holdings);
    }

    // ── 포트폴리오 삭제 ───────────────────────────────────
    @Transactional
    public void delete(Long userId, Long portfolioId) {
        Portfolio portfolio = findPortfolio(portfolioId);
        checkOwnership(portfolio, userId);
        portfolioRepository.delete(portfolio);
        log.info("[Portfolio] 삭제 - userId={}, portfolioId={}", userId, portfolioId);
    }

    // ══════════════════════════════════════════════════════
    // Holdings
    // ══════════════════════════════════════════════════════

    // ── 보유 종목 목록 ────────────────────────────────────
    @Transactional(readOnly = true)
    public List<HoldingResponse> getHoldings(Long userId, Long portfolioId) {
        Portfolio portfolio = findPortfolioWithHoldings(portfolioId);
        checkOwnership(portfolio, userId);

        return portfolio.getHoldings().stream()
                .map(h -> HoldingResponse.of(h, stockPriceService.getCurrentPrice(h.getStockCode())))
                .toList();
    }

    // ── 종목 추가 (매수) ──────────────────────────────────
    @Transactional
    public HoldingResponse addHolding(Long userId, Long portfolioId, AddHoldingRequest request) {
        Portfolio portfolio = findPortfolio(portfolioId);
        checkOwnership(portfolio, userId);

        // 이미 보유 중이면 수량/평균가 업데이트
        PortfolioHolding holding = holdingRepository
                .findByPortfolioIdAndStockCode(portfolioId, request.getStockCode())
                .map(existing -> {
                    existing.buy(request.getQuantity(), request.getPrice());
                    return existing;
                })
                .orElseGet(() -> {
                    // TODO: 종목명은 외부 API 또는 stocks 테이블에서 조회 (현재는 stockCode 임시 사용)
                    String stockName = request.getStockCode(); // 추후 실제 종목명으로 교체
                    return holdingRepository.save(
                            PortfolioHolding.builder()
                                    .portfolio(portfolio)
                                    .stockCode(request.getStockCode())
                                    .stockName(stockName)
                                    .quantity(request.getQuantity())
                                    .averagePrice(request.getPrice())
                                    .build()
                    );
                });

        // 거래 내역 기록
        tradeHistoryRepository.save(
                TradeHistory.builder()
                        .portfolioHolding(holding)
                        .tradeType(TradeHistory.TradeType.BUY)
                        .quantity(request.getQuantity())
                        .price(request.getPrice())
                        .build()
        );

        log.info("[Portfolio] 종목 추가 - portfolioId={}, stock={}, qty={}",
                portfolioId, request.getStockCode(), request.getQuantity());

        BigDecimal currentPrice = stockPriceService.getCurrentPrice(holding.getStockCode());
        return HoldingResponse.of(holding, currentPrice);
    }

    // ── 보유 종목 삭제 ────────────────────────────────────
    @Transactional
    public void deleteHolding(Long userId, Long portfolioId, Long holdingId) {
        Portfolio portfolio = findPortfolio(portfolioId);
        checkOwnership(portfolio, userId);

        PortfolioHolding holding = findHolding(holdingId);
        if (!holding.isOwnedBy(portfolioId)) {
            throw new BusinessException(ErrorCode.HOLDING_NOT_FOUND);
        }
        holdingRepository.delete(holding);
        log.info("[Portfolio] 종목 삭제 - portfolioId={}, holdingId={}", portfolioId, holdingId);
    }

    // ══════════════════════════════════════════════════════
    // Trade History
    // ══════════════════════════════════════════════════════

    // ── 거래 기록 조회 ────────────────────────────────────
    @Transactional(readOnly = true)
    public PageResponse<TradeHistoryResponse> getTrades(Long userId, Long portfolioId,
                                                         Long holdingId, Pageable pageable) {
        Portfolio portfolio = findPortfolio(portfolioId);
        checkOwnership(portfolio, userId);

        PortfolioHolding holding = findHolding(holdingId);
        if (!holding.isOwnedBy(portfolioId)) {
            throw new BusinessException(ErrorCode.HOLDING_NOT_FOUND);
        }

        return PageResponse.of(
                tradeHistoryRepository
                        .findAllByPortfolioHoldingId(holdingId, pageable)
                        .map(TradeHistoryResponse::from)
        );
    }

    // ── 거래 기록 추가 (매수/매도) ────────────────────────
    @Transactional
    public TradeHistoryResponse addTrade(Long userId, Long portfolioId,
                                          Long holdingId, TradeRequest request) {
        Portfolio portfolio = findPortfolio(portfolioId);
        checkOwnership(portfolio, userId);

        PortfolioHolding holding = findHolding(holdingId);
        if (!holding.isOwnedBy(portfolioId)) {
            throw new BusinessException(ErrorCode.HOLDING_NOT_FOUND);
        }

        if (request.getTradeType() == TradeHistory.TradeType.BUY) {
            holding.buy(request.getQuantity(), request.getPrice());
        } else {
            if (holding.getQuantity() < request.getQuantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_QUANTITY);
            }
            holding.sell(request.getQuantity());
        }

        TradeHistory trade = tradeHistoryRepository.save(
                TradeHistory.builder()
                        .portfolioHolding(holding)
                        .tradeType(request.getTradeType())
                        .quantity(request.getQuantity())
                        .price(request.getPrice())
                        .build()
        );

        log.info("[Portfolio] 거래 기록 - portfolioId={}, holdingId={}, type={}, qty={}",
                portfolioId, holdingId, request.getTradeType(), request.getQuantity());
        return TradeHistoryResponse.from(trade);
    }

    // ══════════════════════════════════════════════════════
    // Private helpers
    // ══════════════════════════════════════════════════════

    private BigDecimal calcTotalProfitLossRate(Portfolio portfolio) {
        // 목록 조회 시 현재가 기반 수익률 간단 계산
        var holdings = portfolio.getHoldings();
        if (holdings.isEmpty()) return BigDecimal.ZERO;

        BigDecimal totalInvestment = BigDecimal.ZERO;
        BigDecimal totalCurrent = BigDecimal.ZERO;

        for (PortfolioHolding h : holdings) {
            BigDecimal qty = BigDecimal.valueOf(h.getQuantity());
            totalInvestment = totalInvestment.add(h.getAveragePrice().multiply(qty));
            totalCurrent = totalCurrent.add(stockPriceService.getCurrentPrice(h.getStockCode()).multiply(qty));
        }

        if (totalInvestment.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        return totalCurrent.subtract(totalInvestment)
                .divide(totalInvestment, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private User findUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Portfolio findPortfolio(Long portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));
    }

    private Portfolio findPortfolioWithHoldings(Long portfolioId) {
        return portfolioRepository.findByIdWithHoldings(portfolioId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));
    }

    private PortfolioHolding findHolding(Long holdingId) {
        return holdingRepository.findById(holdingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.HOLDING_NOT_FOUND));
    }

    private void checkOwnership(Portfolio portfolio, Long userId) {
        if (!portfolio.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.PORTFOLIO_ACCESS_DENIED);
        }
    }
}
