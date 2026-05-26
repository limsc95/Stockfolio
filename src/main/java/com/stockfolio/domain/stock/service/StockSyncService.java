package com.stockfolio.domain.stock.service;

import com.stockfolio.domain.stock.entity.Stock;
import com.stockfolio.domain.stock.repository.StockRepository;
import com.stockfolio.infra.external.stock.KrxApiClient;
import com.stockfolio.infra.external.stock.KrxApiClient.KrxStockItem;
import com.stockfolio.infra.external.stock.PublicDataApiClient;
import com.stockfolio.infra.external.stock.PublicDataApiClient.StockItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 종목 데이터 동기화 서비스
 *
 * <p>소스 우선순위:
 * <ol>
 *   <li><b>공공데이터포털</b> (external.publicdata.api-key 설정 시) — 공식, 안정적</li>
 *   <li><b>KRX 크롤링</b> (API 키 미설정 시 폴백) — OTP 기반, 불안정할 수 있음</li>
 * </ol>
 *
 * <p>호출 시점:
 * <ul>
 *   <li>관리자 페이지에서 수동 트리거 (POST /admin/stocks/sync)</li>
 *   <li>필요 시 @Scheduled으로 자동 실행 가능</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockSyncService {

    private final PublicDataApiClient publicDataApiClient;
    private final KrxApiClient        krxApiClient;
    private final StockRepository     stockRepository;

    /**
     * KOSPI + KOSDAQ 전체 종목 동기화
     */
    @Transactional
    public SyncResult syncAll() {
        log.info("[StockSync] 전체 종목 동기화 시작");

        List<StockItem> items = loadStockItems();
        if (items.isEmpty()) {
            log.warn("[StockSync] 소스에서 가져온 종목 데이터가 없습니다.");
            return new SyncResult(0, 0);
        }

        int kospiCount  = 0;
        int kosdaqCount = 0;

        for (StockItem item : items) {
            try {
                Stock.Market market = Stock.Market.valueOf(item.market());
                upsert(item, market);
                if (market == Stock.Market.KOSPI)  kospiCount++;
                else if (market == Stock.Market.KOSDAQ) kosdaqCount++;
            } catch (Exception e) {
                log.warn("[StockSync] 종목 저장 실패 - code={}, error={}", item.code(), e.getMessage());
            }
        }

        log.info("[StockSync] 동기화 완료 - KOSPI={}, KOSDAQ={}, 합계={}",
                 kospiCount, kosdaqCount, kospiCount + kosdaqCount);
        return new SyncResult(kospiCount, kosdaqCount);
    }

    // ── private ──────────────────────────────────────────────────

    /**
     * 종목 목록을 소스에서 로드합니다.
     * 공공데이터포털 API 키가 설정된 경우 우선 사용하고,
     * 미설정 시 KRX 크롤링으로 폴백합니다.
     */
    private List<StockItem> loadStockItems() {
        // ── 1순위: 공공데이터포털 (API 키 필요, 안정적) ───────────────
        if (publicDataApiClient.isConfigured()) {
            log.info("[StockSync] 소스: 공공데이터포털 API");
            List<StockItem> items = publicDataApiClient.fetchAllListedStocks();
            if (!items.isEmpty()) return items;
            log.warn("[StockSync] 공공데이터포털 조회 실패 → KRX 폴백");
        } else {
            log.info("[StockSync] 소스: KRX 크롤링 (공공데이터포털 API 키 미설정)");
        }

        // ── 2순위: KRX 크롤링 폴백 ────────────────────────────────────
        log.info("[StockSync] KRX에서 KOSPI + KOSDAQ 종목 크롤링 시작");
        List<StockItem> result = new ArrayList<>();

        krxApiClient.fetchKospiStocks().stream()
                .map(k -> new StockItem(k.code(), k.name(), "KOSPI", k.sector()))
                .forEach(result::add);

        krxApiClient.fetchKosdaqStocks().stream()
                .map(k -> new StockItem(k.code(), k.name(), "KOSDAQ", k.sector()))
                .forEach(result::add);

        return result;
    }

    private void upsert(StockItem item, Stock.Market market) {
        stockRepository.findById(item.code())
                .ifPresentOrElse(
                        existing -> existing.updateInfo(item.name(), item.sector()),
                        () -> stockRepository.save(
                                Stock.builder()
                                        .code(item.code())
                                        .name(item.name())
                                        .market(market)
                                        .sector(item.sector())
                                        .build()
                        )
                );
    }

    // ── Result DTO ────────────────────────────────────────────────

    public record SyncResult(int kospiCount, int kosdaqCount) {
        public int total() { return kospiCount + kosdaqCount; }
    }
}
