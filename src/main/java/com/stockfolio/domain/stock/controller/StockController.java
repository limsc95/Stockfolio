package com.stockfolio.domain.stock.controller;

import com.stockfolio.domain.stock.dto.StockPriceResponse;
import com.stockfolio.domain.stock.dto.StockResponse;
import com.stockfolio.domain.stock.entity.Stock;
import com.stockfolio.domain.stock.service.StockService;
import com.stockfolio.global.common.ApiResponse;
import com.stockfolio.global.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Stocks", description = "종목 검색 및 시세 조회 API")
@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @Operation(summary = "종목 검색 (인증 불필요)",
               description = "종목명 또는 종목코드로 검색. market 파라미터로 시장 필터링 가능.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StockResponse>>> search(
            @Parameter(description = "검색어 (종목명 or 코드, 2자 이상)")
            @RequestParam String query,
            @Parameter(description = "시장 필터 (KOSPI | KOSDAQ | NYSE | NASDAQ)")
            @RequestParam(required = false) Stock.Market market,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return ResponseEntity.ok(ApiResponse.success(stockService.search(query, market, pageable)));
    }

    @Operation(summary = "종목 현재가 조회 (Redis 캐시, TTL 30초)",
               description = "KIS API 미연동 시 currentPrice=0, priceAvailable=false 로 반환.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/{stockCode}/price")
    public ResponseEntity<ApiResponse<StockPriceResponse>> getPrice(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String stockCode) {

        return ResponseEntity.ok(ApiResponse.success(stockService.getPrice(stockCode)));
    }
}
