package com.stockfolio.global.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 루트 경로 접근 시 Swagger UI로 리다이렉트
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String root() {
        return "redirect:/swagger-ui.html";
    }
}
