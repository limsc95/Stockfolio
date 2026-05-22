package com.stockfolio.domain.portfolio.controller;

import com.stockfolio.domain.portfolio.dto.*;
import com.stockfolio.domain.portfolio.service.PortfolioService;
import com.stockfolio.global.common.ApiResponse;
import com.stockfolio.global.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Portfolios", description = "포트폴리오 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    // ── 포트폴리오 CRUD ───────────────────────────────────

    @Operation(summary = "포트폴리오 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<PortfolioResponse>> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreatePortfolioRequest request) {
        Long userId = extractUserId(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(portfolioService.create(userId, request)));
    }

    @Operation(summary = "내 포트폴리오 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PortfolioSummaryResponse>>> getList(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(portfolioService.getList(userId)));
    }

    @Operation(summary = "포트폴리오 상세 조회")
    @GetMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<PortfolioResponse>> getDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long portfolioId) {
        Long userId = extractUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(portfolioService.getDetail(userId, portfolioId)));
    }

    @Operation(summary = "포트폴리오 수정")
    @PatchMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<PortfolioResponse>> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long portfolioId,
            @Valid @RequestBody UpdatePortfolioRequest request) {
        Long userId = extractUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(portfolioService.update(userId, portfolioId, request)));
    }

    @Operation(summary = "포트폴리오 삭제")
    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long portfolioId) {
        Long userId = extractUserId(userDetails);
        portfolioService.delete(userId, portfolioId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // ── 보유 종목 ─────────────────────────────────────────

    @Operation(summary = "보유 종목 목록 조회")
    @GetMapping("/{portfolioId}/holdings")
    public ResponseEntity<ApiResponse<List<HoldingResponse>>> getHoldings(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long portfolioId) {
        Long userId = extractUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(portfolioService.getHoldings(userId, portfolioId)));
    }

    @Operation(summary = "종목 추가 (매수)")
    @PostMapping("/{portfolioId}/holdings")
    public ResponseEntity<ApiResponse<HoldingResponse>> addHolding(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long portfolioId,
            @Valid @RequestBody AddHoldingRequest request) {
        Long userId = extractUserId(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(portfolioService.addHolding(userId, portfolioId, request)));
    }

    @Operation(summary = "보유 종목 삭제")
    @DeleteMapping("/{portfolioId}/holdings/{holdingId}")
    public ResponseEntity<ApiResponse<Void>> deleteHolding(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long portfolioId,
            @PathVariable Long holdingId) {
        Long userId = extractUserId(userDetails);
        portfolioService.deleteHolding(userId, portfolioId, holdingId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // ── 거래 내역 ─────────────────────────────────────────

    @Operation(summary = "거래 내역 조회")
    @GetMapping("/{portfolioId}/holdings/{holdingId}/trades")
    public ResponseEntity<ApiResponse<PageResponse<TradeHistoryResponse>>> getTrades(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long portfolioId,
            @PathVariable Long holdingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = extractUserId(userDetails);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("tradedAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                portfolioService.getTrades(userId, portfolioId, holdingId, pageable)));
    }

    @Operation(summary = "거래 기록 추가 (매수/매도)")
    @PostMapping("/{portfolioId}/holdings/{holdingId}/trades")
    public ResponseEntity<ApiResponse<TradeHistoryResponse>> addTrade(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long portfolioId,
            @PathVariable Long holdingId,
            @Valid @RequestBody TradeRequest request) {
        Long userId = extractUserId(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        portfolioService.addTrade(userId, portfolioId, holdingId, request)));
    }

    private Long extractUserId(UserDetails userDetails) {
        return Long.parseLong(userDetails.getUsername());
    }
}
