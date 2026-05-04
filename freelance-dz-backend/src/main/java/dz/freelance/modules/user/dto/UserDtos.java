package dz.freelance.modules.user.dto;

import dz.freelance.modules.user.entity.User.ProviderLevel;
import dz.freelance.modules.user.entity.User.ProviderType;
import dz.freelance.modules.user.entity.User.UserRole;
import dz.freelance.modules.category.dto.CategoryDtos.CategoryFlat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class UserDtos {

    // ── Profile response (public) ─────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicProfileResponse {
        private String id;
        private String fullName;
        private String avatarUrl;
        private UserRole role;
        private ProviderType providerType;
        private boolean verified;
        private ProviderLevel providerLevel;
        private String city;
        private String wilaya;
        private String bio;
        private String businessName;
        private String businessDescription;
        private String websiteUrl;
        private Integer yearsExperience;
        private double averageRating;
        private int totalReviews;
        private int completedOrders;
        private Set<String> skills;
        private List<CategoryFlat> categories;
        private LocalDateTime memberSince;
    }

    // ── Profile response (private — own profile) ──────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrivateProfileResponse {
        private String id;
        private String email;
        private String firstName;
        private String lastName;
        private String fullName;
        private String avatarUrl;
        private UserRole role;
        private ProviderType providerType;
        private boolean emailVerified;
        private boolean verified;
        private boolean active;
        private ProviderLevel providerLevel;
        private String phoneNumber;
        private String city;
        private String wilaya;
        private String preferredLanguage;
        private String bio;
        private String businessName;
        private String businessDescription;
        private String websiteUrl;
        private Integer yearsExperience;
        private double averageRating;
        private int totalReviews;
        private int completedOrders;
        private double walletBalance;
        private Set<String> skills;
        private List<CategoryFlat> categories;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // ── Update profile request ────────────────────────────

    @Data
    public static class UpdateProfileRequest {
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private String city;
        private String wilaya;
        private String preferredLanguage;

        // Provider — person
        @Size(max = 1000)
        private String bio;
        private Integer yearsExperience;

        // Provider — organism
        private String businessName;
        @Size(max = 1000)
        private String businessDescription;
        private String websiteUrl;

        // Skills (tags libres)
        private Set<String> skills;
    }

    // ── Update categories ─────────────────────────────────

    @Data
    public static class UpdateCategoriesRequest {
        private List<Long> categoryIds;
    }

    // ── Change password ───────────────────────────────────

    @Data
    public static class ChangePasswordRequest {
        private String currentPassword;
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        private String newPassword;
    }

    // ── Admin user list ───────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminUserRow {
        private String id;
        private String email;
        private String fullName;
        private UserRole role;
        private ProviderType providerType;
        private boolean emailVerified;
        private boolean verified;
        private boolean active;
        private int completedOrders;
        private double averageRating;
        private LocalDateTime createdAt;
    }
}
