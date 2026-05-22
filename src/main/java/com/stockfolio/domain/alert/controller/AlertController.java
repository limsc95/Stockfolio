package com.stockfolio.domain.alert.controller;

import com.stockfolio.domain.alert.dto.AlertHistoryResponse;
import com.stockfolio.domain.alert.dto.AlertResponse;
import com.stockfolio.domain.alert.dto.CreateAlertRequest;
import com.stockfolio.domain.alert.service.AlertService;
import com.stockfolio.global.common.ApiResponse;
import com.stockfolio.global.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Tag(name = "Alerts", description = "가격 알림 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @Operation(summary = "가격 알림 설정",
               description = "TARGET_PRICE: 현재가 ≥ 기준가 / STOP_LOSS: 현재가 ≤ 기준가")
    @PostMapping
    public ResponseEntity<ApiResponse<AlertResponse>> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateAlertRequest request) {
        Long userId = extractUserId(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(alertService.create(userId, request)));
    }

    @Operation(summary = "내 알림 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getList(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "true=발동된 것만 / false=미발동만 / 없으면 전체")
            @RequestParam(required = false) Boolean isTriggered) {
        Long userId = extractUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(alertService.getList(userId, isTriggered)));
    }

    @Operation(summary = "알림 설정 삭제")
    @DeleteMapping("/{alertId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long alertId) {
        Long userId = extractUserId(userDetails);
        alertService.delete(userId, alertId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "내 알림 발송 이력 조회")
    @GetMapping("/histories")
    public ResponseEntity<ApiResponse<PageResponse<AlertHistoryResponse>>> getMyHistories(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = extractUserId(userDetails);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        return ResponseEntity.ok(ApiResponse.success(alertService.getMyHistories(userId, pageable)));
    }

    private Long extractUserId(UserDetails userDetails) {
        return Long.parseLong(userDetails.getUsername());
    }
}
