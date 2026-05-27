package com.stockfolio.domain.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePortfolioRequest {

    @NotBlank(message = "포트폴리오 이름은 필수입니다.")
    @Size(max = 100, message = "포트폴리오 이름은 100자 이하여야 합니다.")
    private String name;

    private String description;
}
