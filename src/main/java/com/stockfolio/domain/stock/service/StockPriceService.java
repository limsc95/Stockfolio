package com.stockfolio.domain.stock.service;

import com.stockfolio.infra.external.stock.KisApiClient;
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
 *  1) Redis 조회 → 있으면 반환 (Cache Hit, TTL 30초)
 *  2) 없으면 KIS API 호출 → Redis 저장 후 반환 (Cache Miss)
 *  3) API도 실패하면 BigDecimal.ZERO 반환 (graceful degradation)
 *
 * Redis Key: stock:price:{stockCode}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockPriceService {

    private static final String PRICE_KEY_PREFIX = "stock:price:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    private final RedisTemplate<String, Object> redisTemplate;
    private final KisApiClient kisApiClient;

    /**
     * 현재가 조회
     */
    public BigDecimal getCurrentPrice(String stockCode) {
        return getFromCache(stockCode)
                .orElseGet(() -> fetchAndCache(stockCode));
    }

    /**
     * 현재가 및 변동 정보 조회 (컨트롤러용)
     */
    public StockPriceResult getPriceResult(String stockCode) {
        BigDecimal price = getCurrentPrice(stockCode);
        // TODO: KIS API 응답에서 변동액/변동률/거래량도 함께 캐싱하도록 확장
        return new StockPriceResult(stockCode, price);
    }

    /**
     * 외부에서 캐시를 직접 저장 (테스트 및 스케줄러용)
     */
    public void cachePrice(String stockCode, BigDecimal price) {
        redisTemplate.opsForValue().set(PRICE_KEY_PREFIX + stockCode, price.toPlainString(), CACHE_TTL);
        log.debug("[StockPrice] 캐시 저장 - stockCode={}, price={}", stockCode, price);
    }

    // ── private ──────────────────────────────────────────

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
        log.debug("[StockPrice] 캐시 미스, API 호출 - stockCode={}", stockCode);
        return kisApiClient.getDomesticStockPrice(stockCode)
                .map(price -> {
                    cachePrice(stockCode, price);
                    return price;
                })
                .orElseGet(() -> {
                    log.warn("[StockPrice] 현재가 조회 불가 - stockCode={}, 0 반환", stockCode);
                    return BigDecimal.ZERO;
                });
    }

    // ── 내부 Result 객체 ─────────────────────────────────
    public record StockPriceResult(String stockCode, BigDecimal currentPrice) {}
}
