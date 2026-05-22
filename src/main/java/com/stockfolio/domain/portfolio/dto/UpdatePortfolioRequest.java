package com.stockfolio.domain.portfolio.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdatePortfolioRequest {

    @Size(max = 100, message = "포트폴리오 이름은 100자 이하여야 합니다.")
    private String name;

    private String description;
}
