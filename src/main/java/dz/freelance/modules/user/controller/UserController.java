package dz.freelance.modules.user.controller;

import dz.freelance.modules.user.dto.UserDtos.*;
import dz.freelance.modules.user.service.UserService;
import dz.freelance.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ── Own profile ───────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PrivateProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyProfile(userDetails.getUsername())));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<PrivateProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateProfile(userDetails.getUsername(), req)));
    }

    @PutMapping("/me/categories")
    public ResponseEntity<ApiResponse<PrivateProfileResponse>> updateCategories(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateCategoriesRequest req) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateCategories(userDetails.getUsername(), req)));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ChangePasswordRequest req) {
        return ResponseEntity.ok(ApiResponse.success(userService.changePassword(userDetails.getUsername(), req)));
    }

    // ── Public profiles ───────────────────────────────────

    @GetMapping("/providers/{userId}")
    public ResponseEntity<ApiResponse<PublicProfileResponse>> getPublicProfile(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getPublicProfile(userId)));
    }

    // ── Admin ─────────────────────────────────────────────

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AdminUserRow>>> listUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(userService.listAllUsers(pageable)));
    }

    @PatchMapping("/admin/users/{userId}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> toggleUser(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.toggleUserActive(userId)));
    }

    @PatchMapping("/admin/users/{userId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> verifyProvider(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.verifyProvider(userId)));
    }
}
