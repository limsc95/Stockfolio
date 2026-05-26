package com.stockfolio.domain.stock.service;

import com.stockfolio.domain.stock.entity.Stock;
import com.stockfolio.domain.stock.repository.StockRepository;
import com.stockfolio.infra.external.stock.KisApiClient;
import com.stockfolio.infra.external.stock.NaverFinanceApiClient;
import com.stockfolio.infra.external.stock.YahooFinanceApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

/**
 * 주식 현재가 조회 서비스 — Cache-Aside 패턴
 *
 * <p>우선순위:
 * <ol>
 *   <li><b>Redis 캐시</b> — TTL 5분, 캐시 히트 시 즉시 반환</li>
 *   <li><b>KIS API</b> — 한국투자증권, appKey 설정 시만 동작 (국내주만)</li>
 *   <li><b>네이버 금융</b> — 무API키, 국내 주식에 매우 안정적</li>
 *   <li><b>Yahoo Finance</b> — 국내·해외 모두 지원하는 최종 폴백</li>
 *   <li>모두 실패 → BigDecimal.ZERO (graceful degradation)</li>
 * </ol>
 *
 * <p>Redis Key: stock:price:{stockCode}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockPriceService {

    private static final String PRICE_KEY_PREFIX = "stock:price:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, Object> redisTemplate;
    private final KisApiClient kisApiClient;
    private final NaverFinanceApiClient naverFinanceApiClient;
    private final YahooFinanceApiClient yahooFinanceApiClient;
    private final StockRepository stockRepository;

    /**
     * 현재가 조회 (종목코드만 — DB에서 시장 정보 자동 조회)
     */
    public BigDecimal getCurrentPrice(String stockCode) {
        return getFromCache(stockCode)
                .orElseGet(() -> fetchAndCache(stockCode));
    }

    /**
     * 현재가 조회 (시장 정보 명시 — DB 조회 생략으로 빠름)
     */
    public BigDecimal getCurrentPrice(String stockCode, Stock.Market market) {
        return getFromCache(stockCode)
                .orElseGet(() -> fetchAndCache(stockCode, market));
    }

    /**
     * 현재가 및 변동 정보 조회 (컨트롤러용)
     */
    public StockPriceResult getPriceResult(String stockCode) {
        BigDecimal price = getCurrentPrice(stockCode);
        return new StockPriceResult(stockCode, price);
    }

    /**
     * 외부에서 캐시를 직접 저장 (테스트 및 스케줄러용)
     */
    public void cachePrice(String stockCode, BigDecimal price) {
        redisTemplate.opsForValue().set(PRICE_KEY_PREFIX + stockCode, price.toPlainString(), CACHE_TTL);
        log.debug("[StockPrice] 캐시 저장 - stockCode={}, price={}", stockCode, price);
    }

    // ── private ──────────────────────────────────────────────────

    private Optional<BigDecimal> getFromCache(String stockCode) {
        Object value = redisTemplate.opsForValue().get(PRICE_KEY_PREFIX + stockCode);
        if (value == null) return Optional.empty();
        try {
            BigDecimal price = new BigDecimal(value.toString());
            log.debug("[StockPrice] 캐시 히트 - stockCode={}, price={}", stockCode, price);
            return Optional.of(price);
        } catch (NumberFormatException e) {
            log.warn("[StockPrice] 캐시 파싱 실패 - stockCode={}, value={}", stockCode, value);
            return Optional.empty();
        }
    }

    private BigDecimal fetchAndCache(String stockCode) {
        Stock.Market market = stockRepository.findById(stockCode)
                .map(Stock::getMarket)
                .orElse(null);
        return fetchAndCache(stockCode, market);
    }

    private BigDecimal fetchAndCache(String stockCode, Stock.Market market) {
        log.debug("[StockPrice] 캐시 미스, API 호출 - stockCode={}, market={}", stockCode, market);

        // ── 1) KIS API (국내, appKey 설정 시) ─────────────────────────
        if (isDomesticMarket(market)) {
            Optional<BigDecimal> kisPrice = kisApiClient.getDomesticStockPrice(stockCode);
            if (kisPrice.isPresent()) {
                cachePrice(stockCode, kisPrice.get());
                log.info("[StockPrice] KIS 조회 성공 - stockCode={}, price={}", stockCode, kisPrice.get());
                return kisPrice.get();
            }
        }

        // ── 2) 네이버 금융 (국내 주식 전용, 무키) ─────────────────────
        if (isDomesticMarket(market) || market == null) {
            Optional<BigDecimal> naverPrice = naverFinanceApiClient.getCurrentPrice(stockCode);
            if (naverPrice.isPresent()) {
                cachePrice(stockCode, naverPrice.get());
                log.info("[StockPrice] 네이버 조회 성공 - stockCode={}, price={}", stockCode, naverPrice.get());
                return naverPrice.get();
            }
        }

        // ── 3) Yahoo Finance (국내·해외 모두, 최종 폴백) ──────────────
        Optional<BigDecimal> yahooPrice = (market != null)
                ? yahooFinanceApiClient.getCurrentPrice(stockCode, market)
                : yahooFinanceApiClient.getCurrentPriceFallback(stockCode);

        if (yahooPrice.isPresent()) {
            cachePrice(stockCode, yahooPrice.get());
            log.info("[StockPrice] Yahoo 조회 성공 - stockCode={}, price={}", stockCode, yahooPrice.get());
            return yahooPrice.get();
        }

        // ── 4) 모두 실패 ───────────────────────────────────────────────
        log.warn("[StockPrice] 현재가 조회 불가 - stockCode={}, 0 반환", stockCode);
        return BigDecimal.ZERO;
    }

    private boolean isDomesticMarket(Stock.Market market) {
        return market == Stock.Market.KOSPI || market == Stock.Market.KOSDAQ;
    }

    // ── Result DTO ────────────────────────────────────────────────

    public record StockPriceResult(String stockCode, BigDecimal currentPrice) {}
}
