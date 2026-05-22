package com.stockfolio.domain.watchlist.controller;

import com.stockfolio.domain.watchlist.dto.WatchListItemResponse;
import com.stockfolio.domain.watchlist.service.WatchListService;
import com.stockfolio.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "WatchList", description = "관심종목 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
public class WatchListController {

    private final WatchListService watchListService;

    @Operation(summary = "관심종목 목록 조회 (현재가 포함)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<WatchListItemResponse>>> getList(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(watchListService.getList(userId)));
    }

    @Operation(summary = "관심종목 추가")
    @PostMapping("/{stockCode}")
    public ResponseEntity<ApiResponse<WatchListItemResponse>> add(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String stockCode) {
        Long userId = extractUserId(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(watchListService.add(userId, stockCode)));
    }

    @Operation(summary = "관심종목 해제")
    @DeleteMapping("/{stockCode}")
    public ResponseEntity<ApiResponse<Void>> remove(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String stockCode) {
        Long userId = extractUserId(userDetails);
        watchListService.remove(userId, stockCode);
        return ResponseEntity.ok(ApiResponse.success());
    }

    private Long extractUserId(UserDetails userDetails) {
        return Long.parseLong(userDetails.getUsername());
    }
}
