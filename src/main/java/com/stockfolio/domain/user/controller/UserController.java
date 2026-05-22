package com.stockfolio.domain.user.controller;

import com.stockfolio.domain.user.dto.ChangePasswordRequest;
import com.stockfolio.domain.user.dto.UpdateProfileRequest;
import com.stockfolio.domain.user.dto.UserResponse;
import com.stockfolio.domain.user.service.UserService;
import com.stockfolio.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users", description = "회원 정보 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(userService.getMe(userId)));
    }

    @Operation(summary = "프로필 수정")
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = extractUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(userService.updateProfile(userId, request)));
    }

    @Operation(summary = "비밀번호 변경")
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = extractUserId(userDetails);
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        userService.withdraw(userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    private Long extractUserId(UserDetails userDetails) {
        return Long.parseLong(userDetails.getUsername());
    }
}
