package com.stockfolio.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenResponse {

    private final String accessToken;
    private final String refreshToken;
    private final long expiresIn;   // Access Token 만료 시간 (초)
}
