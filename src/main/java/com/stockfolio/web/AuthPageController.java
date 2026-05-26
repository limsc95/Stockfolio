package com.stockfolio.web;

import com.stockfolio.domain.user.dto.LoginRequest;
import com.stockfolio.domain.user.dto.SignUpRequest;
import com.stockfolio.domain.user.dto.TokenResponse;
import com.stockfolio.domain.user.service.AuthService;
import com.stockfolio.global.exception.BusinessException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthPageController {

    private final AuthService authService;

    // ── 로그인 페이지 ─────────────────────────────────────
    @GetMapping("/login")
    public String loginPage(@AuthenticationPrincipal UserDetails userDetails,
                            Model model,
                            String error, String success) {
        if (userDetails != null) {
            return "redirect:/home"; // 이미 로그인 중이면 홈으로
        }
        model.addAttribute("error", error);
        model.addAttribute("success", success);
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute LoginRequest form,
                        HttpServletResponse response,
                        RedirectAttributes ra) {
        try {
            TokenResponse tokens = authService.login(form);

            Cookie cookie = new Cookie("SF_TOKEN", tokens.getAccessToken());
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge((int) tokens.getExpiresIn());
            response.addCookie(cookie);

            return "redirect:/home";
        } catch (BusinessException e) {
            ra.addAttribute("error", e.getMessage());
            return "redirect:/login";
        }
    }

    // ── 회원가입 페이지 ────────────────────────────────────
    @GetMapping("/signup")
    public String signupPage(@AuthenticationPrincipal UserDetails userDetails,
                             Model model, String error) {
        if (userDetails != null) {
            return "redirect:/home";
        }
        model.addAttribute("error", error);
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(@ModelAttribute SignUpRequest form,
                         RedirectAttributes ra) {
        try {
            authService.signUp(form);
            ra.addAttribute("success", "회원가입이 완료되었습니다. 로그인해주세요.");
            return "redirect:/login";
        } catch (BusinessException e) {
            ra.addAttribute("error", e.getMessage());
            return "redirect:/signup";
        }
    }

    // ── 로그아웃 ──────────────────────────────────────────
    @PostMapping("/logout")
    public String logout(@AuthenticationPrincipal UserDetails userDetails,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        if (userDetails != null) {
            Long userId = Long.parseLong(userDetails.getUsername());
            String accessToken = resolveTokenFromCookie(request);
            if (accessToken != null) {
                authService.logout(userId, accessToken);
            }
        }
        // SF_TOKEN 쿠키 만료
        Cookie expiredCookie = new Cookie("SF_TOKEN", "");
        expiredCookie.setHttpOnly(true);
        expiredCookie.setPath("/");
        expiredCookie.setMaxAge(0);
        response.addCookie(expiredCookie);

        return "redirect:/login";
    }

    private String resolveTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if ("SF_TOKEN".equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
