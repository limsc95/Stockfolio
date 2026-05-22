package com.stockfolio.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private ApiError error;

    // ── 성공 응답 ───────────────────────────────────────────
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        return response;
    }

    public static <T> ApiResponse<T> success() {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        return response;
    }

    // ── 실패 응답 ───────────────────────────────────────────
    public static <T> ApiResponse<T> fail(ApiError error) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.error = error;
        return response;
    }

    // ── 중첩 에러 클래스 ────────────────────────────────────
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ApiError {
        private String code;
        private String message;

        public static ApiError of(String code, String message) {
            ApiError error = new ApiError();
            error.code = code;
            error.message = message;
            return error;
        }
    }
}
