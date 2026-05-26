package com.stockfolio.domain.admin.controller;

import com.stockfolio.domain.admin.service.AdminService;
import com.stockfolio.domain.alert.entity.AlertHistory;
import com.stockfolio.domain.stock.service.StockSyncService;
import com.stockfolio.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin", description = "관리자 API")
@SecurityRequirement(name = "BearerAuth")
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final StockSyncService stockSyncService;

    // ══════════════════════════════════════════════════════
    // Thymeleaf 페이지
    // ══════════════════════════════════════════════════════

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("stats", adminService.getStats());
        model.addAttribute("activePage", "dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(@RequestParam(required = false) String query,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        PageRequest pageable = PageRequest.of(page, 20, Sort.by("createdAt").descending());
        model.addAttribute("users", adminService.getUsers(query, pageable));
        model.addAttribute("query", query);
        model.addAttribute("activePage", "users");
        return "admin/users";
    }

    @GetMapping("/alerts")
    public String alerts(@RequestParam(required = false) AlertHistory.Status status,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        PageRequest pageable = PageRequest.of(page, 20, Sort.by("sentAt").descending());
        model.addAttribute("histories", adminService.getAlertHistories(status, pageable));
        model.addAttribute("selectedStatus", status);
        model.addAttribute("activePage", "alerts");
        return "admin/alerts";
    }

    // ══════════════════════════════════════════════════════
    // REST API (AJAX / Swagger 용)
    // ══════════════════════════════════════════════════════

    @Operation(summary = "대시보드 통계")
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<ApiResponse<?>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getStats()));
    }

    @Operation(summary = "유저 비활성화")
    @PatchMapping("/api/users/{userId}/deactivate")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId) {
        Long adminId = Long.parseLong(userDetails.getUsername());
        adminService.deactivateUser(adminId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // ── 종목 데이터 동기화 (KRX) ──────────────────────────

    @Operation(summary = "종목 데이터 동기화 (KOSPI + KOSDAQ)")
    @PostMapping("/stocks/sync")
    @ResponseBody
    public ResponseEntity<ApiResponse<?>> syncStocks() {
        StockSyncService.SyncResult result = stockSyncService.syncAll();
        return ResponseEntity.ok(ApiResponse.success(
                java.util.Map.of(
                        "kospi", result.kospiCount(),
                        "kosdaq", result.kosdaqCount(),
                        "total", result.total(),
                        "message", "종목 데이터 동기화 완료"
                )
        ));
    }

    @Operation(summary = "종목 데이터 동기화 (Thymeleaf 폼 POST 용)")
    @PostMapping("/stocks/sync/form")
    public String syncStocksForm(org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            StockSyncService.SyncResult result = stockSyncService.syncAll();
            ra.addFlashAttribute("syncResult",
                    String.format("동기화 완료 — KOSPI %,d건 / KOSDAQ %,d건 / 합계 %,d건",
                            result.kospiCount(), result.kosdaqCount(), result.total()));
        } catch (Exception e) {
            ra.addFlashAttribute("syncError", "동기화 실패: " + e.getMessage());
        }
        return "redirect:/admin";
    }
}
