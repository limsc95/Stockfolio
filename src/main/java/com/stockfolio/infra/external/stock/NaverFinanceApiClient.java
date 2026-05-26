package com.stockfolio.infra.external.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

/**
 * 네이버 금융 모바일 API 클라이언트 (비공식, 무API키)
 *
 * <p>네이버 금융 앱이 사용하는 모바일 API 엔드포인트를 활용합니다.
 * 공식 API가 아니므로 구조가 변경될 수 있으나, 사실상 표준으로 매우 안정적으로 운영됩니다.
 *
 * <p>엔드포인트:
 *   GET https://m.stock.naver.com/api/stock/{code}/basic
 *
 * <p>응답 예시:
 * <pre>
 * {
 *   "stockCode": "005930",
 *   "stockName": "삼성전자",
 *   "closePrice": "57,400",         ← 쉼표 포함
 *   "compareToPreviousClosePrice": "-300",
 *   "fluctuationsRatio": "-0.52",
 *   "marketValue": "342조 5,130억",
 *   ...
 * }
 * </pre>
 */
@Slf4j
@Component
public class NaverFinanceApiClient {

    private static final String BASE_URL = "https://m.stock.naver.com";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public NaverFinanceApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                // 모바일 앱처럼 보이게 User-Agent 설정
                .defaultHeader("User-Agent",
                        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) "
                        + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1")
                .defaultHeader("Accept", "application/json, text/plain, */*")
                .defaultHeader("Accept-Language", "ko-KR,ko;q=0.9")
                .defaultHeader("Referer", "https://m.stock.naver.com/")
                .build();
    }

    /**
     * 종목코드로 현재가(종가) 조회
     *
     * @param stockCode 6자리 종목 코드 (예: "005930")
     * @return 현재가 (조회 실패 시 Optional.empty())
     */
    public Optional<BigDecimal> getCurrentPrice(String stockCode) {
        try {
            String response = webClient.get()
                    .uri("/api/stock/{code}/basic", stockCode)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block();

            if (response == null || response.isBlank()) {
                log.warn("[Naver] 빈 응답 - code={}", stockCode);
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response);

            // closePrice 필드: "57,400" 형태 → 쉼표 제거 후 파싱
            String closePriceStr = root.path("closePrice").asText("").replaceAll("[,\\s]", "");

            if (closePriceStr.isBlank() || "0".equals(closePriceStr)) {
                log.warn("[Naver] 가격 0 또는 누락 - code={}", stockCode);
                return Optional.empty();
            }

            BigDecimal price = new BigDecimal(closePriceStr);
            log.debug("[Naver] 현재가 조회 성공 - code={}, price={}", stockCode, price);
            return Optional.of(price);

        } catch (Exception e) {
            log.warn("[Naver] 현재가 조회 실패 - code={}: {}", stockCode, e.getMessage());
            return Optional.empty();
        }
    }
}
