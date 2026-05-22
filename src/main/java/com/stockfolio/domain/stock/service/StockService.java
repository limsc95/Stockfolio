package com.stockfolio.domain.stock.service;

import com.stockfolio.domain.stock.dto.StockPriceResponse;
import com.stockfolio.domain.stock.dto.StockResponse;
import com.stockfolio.domain.stock.entity.Stock;
import com.stockfolio.domain.stock.repository.StockRepository;
import com.stockfolio.global.common.PageResponse;
import com.stockfolio.global.exception.BusinessException;
import com.stockfolio.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final StockPriceService stockPriceService;

    // ── 종목 검색 ─────────────────────────────────────────
    @Transactional(readOnly = true)
    public PageResponse<StockResponse> search(String query, Stock.Market market, Pageable pageable) {
        var page = (market != null)
                ? stockRepository.searchByNameOrCodeAndMarket(query, market, pageable)
                : stockRepository.searchByNameOrCode(query, pageable);

        return PageResponse.of(page.map(StockResponse::from));
    }

    // ── 현재가 조회 ───────────────────────────────────────
    @Transactional(readOnly = true)
    public StockPriceResponse getPrice(String stockCode) {
        Stock stock = stockRepository.findById(stockCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));

        BigDecimal price = stockPriceService.getCurrentPrice(stockCode);
        log.debug("[Stock] 현재가 조회 - stockCode={}, price={}", stockCode, price);

        return StockPriceResponse.of(stock.getCode(), stock.getName(), price);
    }
}
