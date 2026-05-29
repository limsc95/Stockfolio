package com.stockfolio.infra.external.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class PublicDataApiClient {

    private static final String BASE_URL =
            "https://apis.data.go.kr/1160100/service/GetKrxListedInfoService/getItemInfo";

    private static final int PAGE_SIZE = 3000;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${external.publicdata.api-key:}")
    private String apiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public PublicDataApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();
    }

    /** API 키 설정 여부 확인 */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * KOSPI + KOSDAQ 상장 종목 전체 조회
     *
     * <p>data.go.kr API는 최근 영업일 기준 데이터를 반환합니다.
     * pageNo를 증가시키며 전체 데이터를 수집합니다.
     *
     * @return 종목 목록 (조회 실패 또는 키 미설정 시 빈 리스트)
     */
    public List<StockItem> fetchAllListedStocks() {
        if (!isConfigured()) {
            log.warn("[PublicData] API 키 미설정 → KRX 폴백 사용. " +
                     "external.publicdata.api-key를 application-local.yml에 설정하세요.");
            return Collections.emptyList();
        }

        log.info("[PublicData] 종목 목록 조회 시작");
        List<StockItem> result = new ArrayList<>();
        int pageNo = 1;
        int totalCount = Integer.MAX_VALUE;

        while ((long)(pageNo - 1) * PAGE_SIZE < totalCount) {
            try {
                String response = fetchPage(pageNo);
                if (response == null) break;

                JsonNode body = objectMapper.readTree(response)
                        .path("response").path("body");

                // 첫 페이지에서 전체 개수 확인
                if (pageNo == 1) {
                    totalCount = body.path("totalCount").asInt(0);
                    log.info("[PublicData] 전체 종목 수: {}건", totalCount);
                    if (totalCount == 0) break;
                }

                JsonNode items = body.path("items").path("item");
                if (items.isMissingNode() || !items.isArray() || items.isEmpty()) {
                    log.warn("[PublicData] 페이지 {} 데이터 없음 - 종료", pageNo);
                    break;
                }

                // 첫 페이지 첫 항목 원본 로그 → 필드명/값 확인용 (문제 해결 후 제거 가능)
                if (pageNo == 1 && !items.isEmpty()) {
                    log.debug("[PublicData] 첫 번째 항목 원본: {}", items.get(0));
                }

                for (JsonNode item : items) {
                    StockItem stockItem = parseItem(item);
                    if (stockItem != null) result.add(stockItem);
                }

                log.debug("[PublicData] 페이지 {} 처리 - 누적 {}건", pageNo, result.size());
                pageNo++;

            } catch (Exception e) {
                log.error("[PublicData] 페이지 {} 처리 실패", pageNo, e);
                break;
            }
        }

        log.info("[PublicData] 종목 목록 조회 완료 - 총 {}건", result.size());
        return result;
    }

    // ── private ──────────────────────────────────────────────────

    private String fetchPage(int pageNo) {
        try {
            // data.go.kr는 serviceKey를 UriComponentsBuilder로 직접 주입해야
            // 이중 인코딩 없이 정상 동작 (queryParam()은 자동 인코딩하므로 사용 안 함)
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(BASE_URL)
                    .queryParam("serviceKey", apiKey)
                    .queryParam("numOfRows", PAGE_SIZE)
                    .queryParam("pageNo", pageNo)
                    .queryParam("resultType", "json")
                    .queryParam("basDt", LocalDate.now().minusDays(1).format(DATE_FMT))
                    .build(true)   // ← encode=false: 이미 인코딩된 키 그대로 사용
                    .toUri();

            return webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("[PublicData] HTTP 요청 실패 - pageNo={}: {}", pageNo, e.getMessage());
            return null;
        }
    }

    private StockItem parseItem(JsonNode item) {
        try {
            // 공공데이터포털은 단축코드 앞에 "A" 접두사를 붙여 반환 (예: "A005930")
            // 알파벳 1자리 + 6자리 숫자 형태이면 접두사를 제거하여 표준 6자리 코드로 변환
            String rawCode = item.path("srtnCd").asText("").trim();
            String code = (rawCode.length() == 7 && Character.isLetter(rawCode.charAt(0)))
                    ? rawCode.substring(1)
                    : rawCode;

            String name   = item.path("itmsNm").asText("").trim();
            String market = item.path("mrktCtg").asText("").trim().toUpperCase();

            // 6자리 숫자 코드만 수락 (ETF·ELW 등 제외)
            if (!code.matches("\\d{6}")) return null;
            if (name.isBlank()) return null;
            if (!market.equals("KOSPI") && !market.equals("KOSDAQ")) return null;

            return new StockItem(code, name, market, null);
        } catch (Exception e) {
            log.warn("[PublicData] 종목 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    // ── DTO ──────────────────────────────────────────────────────

    /**
     * @param code   6자리 단축코드
     * @param name   종목명
     * @param market "KOSPI" 또는 "KOSDAQ"
     * @param sector 업종 (공공데이터 API에서는 미제공, null)
     */
    public record StockItem(String code, String name, String market, String sector) {}
}
