package com.stockfolio.domain.watchlist.service;

import com.stockfolio.domain.stock.entity.Stock;
import com.stockfolio.domain.stock.repository.StockRepository;
import com.stockfolio.domain.stock.service.StockPriceService;
import com.stockfolio.domain.user.entity.User;
import com.stockfolio.domain.user.repository.UserRepository;
import com.stockfolio.domain.watchlist.dto.WatchListItemResponse;
import com.stockfolio.domain.watchlist.entity.WatchListItem;
import com.stockfolio.domain.watchlist.repository.WatchListRepository;
import com.stockfolio.global.exception.BusinessException;
import com.stockfolio.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchListService {

    private final WatchListRepository watchListRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;
    private final StockPriceService stockPriceService;

    // ── 관심종목 목록 조회 ────────────────────────────────
    @Transactional(readOnly = true)
    public List<WatchListItemResponse> getList(Long userId) {
        return watchListRepository.findAllByUserIdWithStock(userId).stream()
                .map(item -> WatchListItemResponse.of(
                        item,
                        stockPriceService.getCurrentPrice(item.getStock().getCode())
                ))
                .toList();
    }

    // ── 관심종목 추가 ─────────────────────────────────────
    @Transactional
    public WatchListItemResponse add(Long userId, String stockCode) {
        if (watchListRepository.existsByUserIdAndStockCode(userId, stockCode)) {
            throw new BusinessException(ErrorCode.WATCHLIST_ALREADY_EXISTS);
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Stock stock = stockRepository.findById(stockCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));

        WatchListItem item = WatchListItem.builder()
                .user(user)
                .stock(stock)
                .build();

        WatchListItem saved = watchListRepository.save(item);
        log.info("[WatchList] 추가 - userId={}, stockCode={}", userId, stockCode);

        return WatchListItemResponse.of(
                saved,
                stockPriceService.getCurrentPrice(stockCode)
        );
    }

    // ── 관심종목 해제 ─────────────────────────────────────
    @Transactional
    public void remove(Long userId, String stockCode) {
        WatchListItem item = watchListRepository.findByUserIdAndStockCode(userId, stockCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.WATCHLIST_NOT_FOUND));

        watchListRepository.delete(item);
        log.info("[WatchList] 해제 - userId={}, stockCode={}", userId, stockCode);
    }
}
