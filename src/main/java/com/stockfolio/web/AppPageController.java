package com.stockfolio.web;

import com.stockfolio.domain.alert.dto.AlertResponse;
import com.stockfolio.domain.alert.dto.CreateAlertRequest;
import com.stockfolio.domain.alert.entity.PriceAlert;
import com.stockfolio.domain.alert.service.AlertService;
import com.stockfolio.domain.portfolio.dto.CreatePortfolioRequest;
import com.stockfolio.domain.portfolio.dto.PortfolioResponse;
import com.stockfolio.domain.portfolio.dto.PortfolioSummaryResponse;
import com.stockfolio.domain.portfolio.service.PortfolioService;
import com.stockfolio.domain.stock.entity.Stock;
import com.stockfolio.domain.stock.service.StockService;
import com.stockfolio.domain.user.service.UserService;
import com.stockfolio.domain.watchlist.service.WatchListService;
import com.stockfolio.global.common.PageResponse;
import com.stockfolio.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AppPageController {

    private final PortfolioService portfolioService;
    private final StockService stockService;
    private final WatchListService watchListService;
    private final AlertService alertService;
    private final UserService userService;

    // ── 모든 페이지에 공통으로 me(로그인 사용자 정보) 주입 ──
    @ModelAttribute("me")
    public com.stockfolio.domain.user.dto.UserResponse currentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return null;
        try {
            return userService.getMe(Long.parseLong(userDetails.getUsername()));
        } catch (Exception e) {
            return null;
        }
    }

    // ── 홈: 포트폴리오 목록 ──────────────────────────────
    @GetMapping("/home")
    public String home(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Long userId = uid(userDetails);
        List<PortfolioSummaryResponse> portfolios = portfolioService.getList(userId);
        model.addAttribute("portfolios", portfolios);
        return "app/home";
    }

    // ── 포트폴리오 생성 (POST → PRG) ─────────────────────
    @PostMapping("/home/portfolio")
    public String createPortfolio(@AuthenticationPrincipal UserDetails userDetails,
                                  @RequestParam String name,
                                  @RequestParam(required = false) String description,
                                  RedirectAttributes ra) {
        try {
            CreatePortfolioRequest req = new CreatePortfolioRequest();
            req.setName(name);
            req.setDescription(description);
            portfolioService.create(uid(userDetails), req);
            ra.addFlashAttribute("successMsg", "포트폴리오가 생성되었습니다.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/home";
    }

    // ── 포트폴리오 삭제 ───────────────────────────────────
    @PostMapping("/home/portfolio/{id}/delete")
    public String deletePortfolio(@AuthenticationPrincipal UserDetails userDetails,
                                  @PathVariable Long id,
                                  RedirectAttributes ra) {
        try {
            portfolioService.delete(uid(userDetails), id);
            ra.addFlashAttribute("successMsg", "포트폴리오가 삭제되었습니다.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/home";
    }

    // ── 포트폴리오 상세 ───────────────────────────────────
    @GetMapping("/portfolios/{id}")
    public String portfolioDetail(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model) {
        Long userId = uid(userDetails);
        PortfolioResponse portfolio = portfolioService.getDetail(userId, id);
        model.addAttribute("portfolio", portfolio);
        return "app/portfolio-detail";
    }

    // ── 보유 종목 추가 (POST → PRG) ───────────────────────
    @PostMapping("/portfolios/{id}/holdings")
    public String addHolding(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails userDetails,
                             @RequestParam String stockCode,
                             @RequestParam int quantity,
                             @RequestParam BigDecimal price,
                             RedirectAttributes ra) {
        try {
            com.stockfolio.domain.portfolio.dto.AddHoldingRequest req =
                    new com.stockfolio.domain.portfolio.dto.AddHoldingRequest();
            req.setStockCode(stockCode);
            req.setQuantity(quantity);
            req.setPrice(price);
            portfolioService.addHolding(uid(userDetails), id, req);
            ra.addFlashAttribute("successMsg", "종목이 추가되었습니다.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/portfolios/" + id;
    }

    // ── 종목 검색 ─────────────────────────────────────────
    @GetMapping("/stocks")
    public String stocks(@RequestParam(required = false) String query,
                         @RequestParam(required = false) String market,
                         Model model) {
        model.addAttribute("query", query);
        model.addAttribute("market", market);

        if (StringUtils.hasText(query)) {
            Stock.Market mkt = null;
            if (StringUtils.hasText(market)) {
                try { mkt = Stock.Market.valueOf(market); } catch (IllegalArgumentException ignored) {}
            }
            PageResponse<?> result = stockService.search(query, mkt, PageRequest.of(0, 30));
            model.addAttribute("stocks", result.getContent());
        }
        return "app/stocks";
    }

    // ── 관심종목 ──────────────────────────────────────────
    @GetMapping("/watchlist")
    public String watchlist(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("items", watchListService.getList(uid(userDetails)));
        return "app/watchlist";
    }

    @PostMapping("/watchlist/{stockCode}")
    public String addWatchlist(@PathVariable String stockCode,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes ra) {
        try {
            watchListService.add(uid(userDetails), stockCode);
            ra.addFlashAttribute("successMsg", "관심종목에 추가되었습니다.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/watchlist";
    }

    @PostMapping("/watchlist/{stockCode}/delete")
    public String removeWatchlist(@PathVariable String stockCode,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes ra) {
        try {
            watchListService.remove(uid(userDetails), stockCode);
            ra.addFlashAttribute("successMsg", "관심종목에서 제거되었습니다.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/watchlist";
    }

    // ── 알림 설정 ─────────────────────────────────────────
    @GetMapping("/alerts")
    public String alerts(@AuthenticationPrincipal UserDetails userDetails,
                         @RequestParam(required = false) Boolean triggered,
                         Model model) {
        Long userId = uid(userDetails);
        List<AlertResponse> alerts = alertService.getList(userId, triggered);
        model.addAttribute("alerts", alerts);
        model.addAttribute("triggered", triggered);
        return "app/alerts";
    }

    @PostMapping("/alerts")
    public String createAlert(@AuthenticationPrincipal UserDetails userDetails,
                              @RequestParam String stockCode,
                              @RequestParam String alertType,
                              @RequestParam BigDecimal targetPrice,
                              RedirectAttributes ra) {
        try {
            CreateAlertRequest req = new CreateAlertRequest();
            req.setStockCode(stockCode);
            req.setAlertType(PriceAlert.AlertType.valueOf(alertType));
            req.setTargetPrice(targetPrice);
            alertService.create(uid(userDetails), req);
            ra.addFlashAttribute("successMsg", "알림이 설정되었습니다.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/{id}/delete")
    public String deleteAlert(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes ra) {
        try {
            alertService.delete(uid(userDetails), id);
            ra.addFlashAttribute("successMsg", "알림이 삭제되었습니다.");
        } catch (BusinessException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/alerts";
    }

    // ── 헬퍼 ──────────────────────────────────────────────
    private Long uid(UserDetails userDetails) {
        return Long.parseLong(userDetails.getUsername());
    }
}
