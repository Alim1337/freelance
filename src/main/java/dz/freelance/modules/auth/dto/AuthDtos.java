package dz.freelance.modules.auth.dto;

import dz.freelance.modules.user.entity.User.UserRole;
import dz.freelance.modules.user.entity.User.ProviderType;
import jakarta.validation.constraints.*;
import lombok.*;

public class AuthDtos {

    // ── Register ─────────────────────────────────────────

    @Data
    public static class RegisterRequest {
        @NotBlank @Email
        private String email;

        @NotBlank @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        private String password;

        @NotNull
        private UserRole role; // CLIENT or PROVIDER

        // Champs communs
        @NotBlank
        private String firstName;

        @NotBlank
        private String lastName;

        private String phoneNumber;
        private String city;
        private String wilaya;

        // Champs PROVIDER uniquement
        private ProviderType providerType; // PERSON ou ORGANISM

        // Si ORGANISM
        private String businessName; // Nom commercial / nom d'organisme

        private String businessDescription;
        private String websiteUrl;
    }

    // ── Login ─────────────────────────────────────────────

    @Data
    public static class LoginRequest {
        @NotBlank @Email
        private String email;

        @NotBlank
        private String password;
    }

    // ── Responses ─────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private UserSummary user;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private String id;
        private String email;
        private String fullName;
        private String avatarUrl;
        private UserRole role;
        private ProviderType providerType;
        private boolean emailVerified;
        private boolean verified;
    }

    // ── OTP / Email verification ──────────────────────────

    @Data
    public static class VerifyEmailRequest {
        @NotBlank
        private String email;
        @NotBlank @Size(min = 6, max = 6)
        private String otp;
    }

    @Data
    public static class ResendOtpRequest {
        @NotBlank @Email
        private String email;
    }

    // ── Refresh token ─────────────────────────────────────

    @Data
    public static class RefreshTokenRequest {
        @NotBlank
        private String refreshToken;
    }

    // ── Password reset ────────────────────────────────────

    @Data
    public static class ForgotPasswordRequest {
        @NotBlank @Email
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank @Email
        private String email;
        @NotBlank @Size(min = 6, max = 6)
        private String otp;
        @NotBlank @Size(min = 8)
        private String newPassword;
    }
}
