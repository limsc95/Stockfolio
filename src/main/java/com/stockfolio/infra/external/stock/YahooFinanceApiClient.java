package com.stockfolio.infra.external.stock;

import com.stockfolio.domain.stock.entity.Stock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Yahoo Finance 비공식 API 클라이언트 (무료, API 키 불필요)
 *
 * 엔드포인트: https://query1.finance.yahoo.com/v8/finance/chart/{symbol}
 *
 * 시장별 심볼 변환:
 *   KOSPI  → {code}.KS   (예: 005930.KS)
 *   KOSDAQ → {code}.KQ   (예: 035720.KQ)
 *   NYSE   → {code}      (예: AAPL)
 *   NASDAQ → {code}      (예: MSFT)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YahooFinanceApiClient {

    private static final String YAHOO_BASE_URL = "https://query1.finance.yahoo.com";
    private final WebClient stockApiWebClient;

    /**
     * 현재가 조회
     * @param stockCode 종목코드
     * @param market    시장 (KOSPI/KOSDAQ/NYSE/NASDAQ)
     * @return 현재가. 조회 실패 시 Optional.empty()
     */
    public Optional<BigDecimal> getCurrentPrice(String stockCode, Stock.Market market) {
        String symbol = toYahooSymbol(stockCode, market);
        return fetchPrice(symbol);
    }

    /**
     * 시장 정보 없이 코드만으로 조회 (KOSPI → KOSDAQ 순으로 시도)
     */
    public Optional<BigDecimal> getCurrentPriceFallback(String stockCode) {
        // 숫자 6자리 → 한국 주식 (KOSPI 먼저, 실패 시 KOSDAQ)
        if (stockCode.matches("\\d{6}")) {
            Optional<BigDecimal> kospi = fetchPrice(stockCode + ".KS");
            if (kospi.isPresent()) return kospi;
            return fetchPrice(stockCode + ".KQ");
        }
        // 영문 코드 → 미국 주식
        return fetchPrice(stockCode);
    }

    // ── private ──────────────────────────────────────────

    private String toYahooSymbol(String stockCode, Stock.Market market) {
        if (market == null) return stockCode;
        return switch (market) {
            case KOSPI  -> stockCode + ".KS";
            case KOSDAQ -> stockCode + ".KQ";
            case NYSE, NASDAQ -> stockCode;
        };
    }

    @SuppressWarnings("unchecked")
    private Optional<BigDecimal> fetchPrice(String symbol) {
        try {
            Map<String, Object> response = stockApiWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("query1.finance.yahoo.com")
                            .path("/v8/finance/chart/" + symbol)
                            .queryParam("interval", "1d")
                            .queryParam("range", "1d")
                            .build())
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(e -> {
                        log.warn("[Yahoo] 조회 실패 - symbol={}, error={}", symbol, e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (response == null) return Optional.empty();

            Map<String, Object> chart = (Map<String, Object>) response.get("chart");
            if (chart == null) return Optional.empty();

            List<Map<String, Object>> results = (List<Map<String, Object>>) chart.get("result");
            if (results == null || results.isEmpty()) return Optional.empty();

            Map<String, Object> meta = (Map<String, Object>) results.get(0).get("meta");
            if (meta == null) return Optional.empty();

            // regularMarketPrice: 현재 거래중 가격 (장중/장후 모두 가능)
            Object priceObj = meta.get("regularMarketPrice");
            if (priceObj == null) return Optional.empty();

            BigDecimal price = new BigDecimal(priceObj.toString());
            log.debug("[Yahoo] 현재가 조회 성공 - symbol={}, price={}", symbol, price);
            return Optional.of(price);

        } catch (Exception e) {
            log.error("[Yahoo] 파싱 오류 - symbol={}", symbol, e);
            return Optional.empty();
        }
    }
}
