package com.stockfolio.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── 공통 ─────────────────────────────────────────────
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "서버 내부 오류가 발생했습니다."),

    // ── 인증/인가 ─────────────────────────────────────────
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "만료된 토큰입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_004", "접근 권한이 없습니다."),

    // ── 회원 ─────────────────────────────────────────────
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "회원을 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_002", "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USER_003", "현재 비밀번호가 일치하지 않습니다."),
    DEACTIVATED_USER(HttpStatus.FORBIDDEN, "USER_004", "비활성화된 계정입니다."),

    // ── 포트폴리오 ────────────────────────────────────────
    PORTFOLIO_NOT_FOUND(HttpStatus.NOT_FOUND, "PORTFOLIO_001", "포트폴리오를 찾을 수 없습니다."),
    PORTFOLIO_ACCESS_DENIED(HttpStatus.FORBIDDEN, "PORTFOLIO_002", "해당 포트폴리오에 접근 권한이 없습니다."),

    // ── 보유 종목 ─────────────────────────────────────────
    HOLDING_NOT_FOUND(HttpStatus.NOT_FOUND, "HOLDING_001", "보유 종목을 찾을 수 없습니다."),
    HOLDING_ALREADY_EXISTS(HttpStatus.CONFLICT, "HOLDING_002", "이미 포트폴리오에 등록된 종목입니다."),
    INSUFFICIENT_QUANTITY(HttpStatus.BAD_REQUEST, "HOLDING_003", "보유 수량이 부족합니다."),

    // ── 종목 ─────────────────────────────────────────────
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "STOCK_001", "종목을 찾을 수 없습니다."),
    STOCK_PRICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "STOCK_002", "현재 시세를 불러올 수 없습니다."),

    // ── 관심종목 ──────────────────────────────────────────
    WATCHLIST_ALREADY_EXISTS(HttpStatus.CONFLICT, "WATCHLIST_001", "이미 관심종목에 등록된 종목입니다."),
    WATCHLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "WATCHLIST_002", "관심종목에 등록되지 않은 종목입니다."),

    // ── 알림 ─────────────────────────────────────────────
    ALERT_NOT_FOUND(HttpStatus.NOT_FOUND, "ALERT_001", "알림 설정을 찾을 수 없습니다."),
    ALERT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ALERT_002", "해당 알림에 접근 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
