package com.stockfolio.infra.external.stock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * KRX(한국거래소) 데이터포털 클라이언트
 *
 * KRX는 3단계 흐름으로 데이터를 제공합니다:
 *   Step 0) GET  메인 페이지          → 세션 쿠키 수립 (필수!)
 *   Step 1) POST ValidTypeParser.cmd  → OTP 토큰 발급
 *   Step 2) POST fileDwldServlet      → OTP로 CSV 다운로드
 *
 * Step 0 없이 OTP 요청 시 → "서버에 내부적으로 에러가 발생하였습니다" HTML 반환
 */
@Slf4j
@Component
public class KrxApiClient {

    private static final String BASE_URL     = "https://data.krx.co.kr";
    private static final String PAGE_URL     = BASE_URL + "/contents/MDC/MDI/mdiLoader/index.cmd?menuId=MDC0201020201";
    private static final String OTP_URL      = BASE_URL + "/comm/util/ValidTypeParser.cmd";
    private static final String DOWNLOAD_URL = BASE_URL + "/comm/fileDwldServlet";
    private static final String BLD          = "dbms/MDC/STAT/standard/MDCSTAT01901";

    private static final Charset EUC_KR = Charset.forName("EUC-KR");

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    /** KOSPI 전체 종목 조회 */
    public List<KrxStockItem> fetchKospiStocks() {
        return fetchStocks("STK");
    }

    /** KOSDAQ 전체 종목 조회 */
    public List<KrxStockItem> fetchKosdaqStocks() {
        return fetchStocks("KSQ");
    }

    // ─────────────────────────────────────────────────────────
    // private
    // ─────────────────────────────────────────────────────────

    private List<KrxStockItem> fetchStocks(String marketId) {
        try {
            log.info("[KRX] 종목 조회 시작 - marketId={}", marketId);

            // 요청마다 독립된 쿠키 저장소 사용
            CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
            HttpClient client = HttpClient.newBuilder()
                    .cookieHandler(cookieManager)
                    .connectTimeout(Duration.ofSeconds(15))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            // ── Step 0: 세션 쿠키 수립 (브라우저처럼 페이지 먼저 방문) ──
            establishSession(client);

            // ── Step 1: OTP 토큰 발급 ─────────────────────────────────
            String otp = getOtp(client, marketId);
            if (otp == null) {
                log.error("[KRX] OTP 발급 실패 - marketId={}", marketId);
                return Collections.emptyList();
            }
            log.debug("[KRX] OTP 발급 성공 - marketId={}", marketId);

            // ── Step 2: CSV 다운로드 ───────────────────────────────────
            byte[] csvBytes = downloadCsv(client, otp);
            if (csvBytes == null || csvBytes.length == 0) {
                log.error("[KRX] CSV 다운로드 실패 - marketId={}", marketId);
                return Collections.emptyList();
            }

            // ── Step 3: CSV 파싱 ───────────────────────────────────────
            List<KrxStockItem> items = parseCsv(csvBytes, marketId);
            log.info("[KRX] 종목 조회 완료 - marketId={}, count={}", marketId, items.size());
            return items;

        } catch (Exception e) {
            log.error("[KRX] 종목 조회 실패 - marketId={}", marketId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Step 0: KRX 페이지를 방문해 세션 쿠키를 얻습니다.
     * 이 단계 없이 OTP 요청 시 서버가 에러 HTML을 반환합니다.
     */
    private void establishSession(HttpClient client) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(PAGE_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .GET()
                    .build();
            HttpResponse<Void> res = client.send(req, HttpResponse.BodyHandlers.discarding());
            log.debug("[KRX] 세션 수립 - status={}", res.statusCode());
        } catch (Exception e) {
            log.warn("[KRX] 세션 수립 실패 (계속 진행): {}", e.getMessage());
        }
    }

    /**
     * Step 1: OTP 토큰 발급
     * 성공 시 JSON {"RESULT":"success","OTP":"..."} 반환
     */
    private String getOtp(HttpClient client, String marketId) {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

            // bld 값의 '/' 를 %2F 로 인코딩해야 서버가 올바르게 파싱함
            String body = "bld=" + URLEncoder.encode(BLD, StandardCharsets.UTF_8)
                    + "&locale=ko_KR"
                    + "&mktId=" + marketId
                    + "&trdDd=" + today
                    + "&share=1"
                    + "&money=1"
                    + "&csvxls_isNo=false";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(OTP_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Referer", PAGE_URL)
                    .header("User-Agent", USER_AGENT)
                    .header("Origin", BASE_URL)
                    .header("Accept", "application/json, text/javascript, */*; q=0.01")
                    .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

            log.debug("[KRX] OTP raw = {}", res.body());

            return extractOtp(res.body());

        } catch (Exception e) {
            log.error("[KRX] OTP 요청 오류", e);
            return null;
        }
    }

    private String extractOtp(String body) {
        if (body == null || body.isBlank()) return null;

        // JSON 형태
        Pattern p = Pattern.compile("\"OTP\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(body);
        if (m.find()) return m.group(1);

        // plain text (OTP만 오는 경우)
        if (!body.contains("{") && body.length() > 5) {
            return body.trim();
        }

        return null;
    }

    /**
     * Step 2: OTP로 CSV 파일 다운로드
     */
    private byte[] downloadCsv(HttpClient client, String otp) {
        try {
            String body = "code=" + URLEncoder.encode(otp, StandardCharsets.UTF_8);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(DOWNLOAD_URL))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Referer", PAGE_URL)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/csv,application/octet-stream,*/*")
                    .header("Origin", BASE_URL)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<byte[]> res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            log.debug("[KRX] CSV 다운로드 - status={}, bytes={}", res.statusCode(), res.body().length);

            return (res.statusCode() == 200) ? res.body() : null;

        } catch (Exception e) {
            log.error("[KRX] CSV 다운로드 오류", e);
            return null;
        }
    }

    /**
     * Step 3: CSV 파싱 (EUC-KR 인코딩)
     *
     * KRX CSV 헤더 예시:
     *   종목코드, 단축코드, 한글종목명, 한글종목약명, ..., 업종명, ...
     */
    private List<KrxStockItem> parseCsv(byte[] csvBytes, String marketId) {
        List<KrxStockItem> items = new ArrayList<>();
        try {
            // EUC-KR 디코딩 (KRX CSV 표준 인코딩)
            String content = new String(csvBytes, EUC_KR);
            // BOM 제거
            if (content.startsWith("﻿")) content = content.substring(1);

            BufferedReader reader = new BufferedReader(new StringReader(content));
            String headerLine = reader.readLine();
            if (headerLine == null) return items;

            String[] headers = splitCsvLine(headerLine);
            log.debug("[KRX] CSV 헤더 ({}) = {}", marketId, List.of(headers));

            // 컬럼 인덱스 자동 탐지
            int codeIdx   = findIdx(headers, "단축코드", "ISU_SRT_CD");
            int nameIdx   = findIdx(headers, "한글종목약명", "ISU_ABBRV", "한글종목명", "ISU_NM");
            int sectorIdx = findIdx(headers, "업종명", "IDX_IND_NM", "SECT_TP_NM");

            // 헤더 감지 실패 시 KRX 표준 컬럼 위치로 폴백
            if (codeIdx < 0) codeIdx = 1;  // 2번째 컬럼 = 단축코드
            if (nameIdx < 0) nameIdx = 3;  // 4번째 컬럼 = 한글종목약명

            log.debug("[KRX] 컬럼 인덱스 - code={}, name={}, sector={}", codeIdx, nameIdx, sectorIdx);

            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                String[] cols = splitCsvLine(trimmed);
                if (cols.length <= Math.max(codeIdx, nameIdx)) continue;

                String code   = clean(cols[codeIdx]);
                String name   = clean(cols[nameIdx]);
                String sector = (sectorIdx >= 0 && sectorIdx < cols.length)
                        ? clean(cols[sectorIdx]) : null;

                if (!code.matches("\\d{6}")) continue;   // 6자리 숫자 코드만
                if (name.isEmpty()) continue;
                if (sector != null && (sector.isEmpty() || "-".equals(sector))) sector = null;

                items.add(new KrxStockItem(code, name, sector));
            }
        } catch (Exception e) {
            log.error("[KRX] CSV 파싱 오류 - marketId={}", marketId, e);
        }
        return items;
    }

    // ── 유틸리티 ─────────────────────────────────────────────

    /** JSON 문자열에서 특정 키의 값 추출 (간단한 정규식 파싱) */
    private String extractJsonValue(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    /** CSV 한 줄 파싱 (따옴표 내 쉼표 처리) */
    private String[] splitCsvLine(String line) {
        List<String> cols = new ArrayList<>();
        boolean inQuote = false;
        StringBuilder sb = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuote = !inQuote;
            } else if (c == ',' && !inQuote) {
                cols.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        cols.add(sb.toString());
        return cols.toArray(new String[0]);
    }

    /** 헤더 배열에서 후보 문자열을 포함하는 첫 번째 인덱스 반환 */
    private int findIdx(String[] headers, String... candidates) {
        for (String candidate : candidates) {
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].trim().contains(candidate)) return i;
            }
        }
        return -1;
    }

    private String clean(String s) {
        return s == null ? "" : s.replaceAll("[\\r\\n]", "").trim();
    }

    public record KrxStockItem(String code, String name, String sector) {}
}
