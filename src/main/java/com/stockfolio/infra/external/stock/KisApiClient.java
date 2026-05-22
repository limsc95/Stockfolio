package com.stockfolio.infra.external.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * 한국투자증권(KIS) Developers API 클라이언트
 *
 * 모의계좌 API 기반 (무료)
 * 공식 문서: https://apiportal.koreainvestment.com/
 *
 * 현재 구현:
 *   - 국내 주식 현재가 조회 (FHKST01010100)
 *
 * 사용 전 준비:
 *   1. KIS Developers 가입 후 앱 생성
 *   2. appKey / appSecret 발급
 *   3. /oauth2/tokenP 로 access_token 발급
 *   4. application-local.yml 에 kis.* 값 입력
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KisApiClient {

    private static final String KIS_BASE_URL = "https://openapivts.koreainvestment.com:29443"; // 모의투자
    // private static final String KIS_BASE_URL = "https://openapi.koreainvestment.com:9443"; // 실전

    private final WebClient stockApiWebClient;

    @Value("${kis.app-key:}")
    private String appKey;

    @Value("${kis.app-secret:}")
    private String appSecret;

    @Value("${kis.access-token:}")
    private String accessToken;

    /**
     * 국내 주식 현재가 조회
     * @param stockCode 종목코드 (6자리, 예: 005930)
     * @return 현재가. 조회 실패 시 Optional.empty()
     */
    public Optional<BigDecimal> getDomesticStockPrice(String stockCode) {
        if (!isConfigured()) {
            log.debug("[KIS] API 키 미설정 - stockCode={}", stockCode);
            return Optional.empty();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = stockApiWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("openapivts.koreainvestment.com")
                            .port(29443)
                            .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", stockCode)
                            .build())
                    .header("authorization", "Bearer " + accessToken)
                    .header("appkey", appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id", "FHKST01010100")
                    .header("custtype", "P")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(e -> {
                        log.error("[KIS] 현재가 조회 실패 - stockCode={}, error={}", stockCode, e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            if (response == null) return Optional.empty();

            @SuppressWarnings("unchecked")
            Map<String, Object> output = (Map<String, Object>) response.get("output");
            if (output == null) return Optional.empty();

            String priceStr = (String) output.get("stck_prpr"); // 주식 현재가
            if (priceStr == null || priceStr.isBlank()) return Optional.empty();

            return Optional.of(new BigDecimal(priceStr));

        } catch (Exception e) {
            log.error("[KIS] 현재가 파싱 오류 - stockCode={}", stockCode, e);
            return Optional.empty();
        }
    }

    private boolean isConfigured() {
        return appKey != null && !appKey.isBlank()
                && appSecret != null && !appSecret.isBlank()
                && accessToken != null && !accessToken.isBlank();
    }
}
