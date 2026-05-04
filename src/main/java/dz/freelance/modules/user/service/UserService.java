package dz.freelance.modules.user.service;

import dz.freelance.modules.category.dto.CategoryDtos.CategoryFlat;
import dz.freelance.modules.category.entity.Category;
import dz.freelance.modules.category.repository.CategoryRepository;
import dz.freelance.modules.user.dto.UserDtos.*;
import dz.freelance.modules.user.entity.User;
import dz.freelance.modules.user.repository.UserRepository;
import dz.freelance.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Get own profile ───────────────────────────────────

    public PrivateProfileResponse getMyProfile(String email) {
        User user = userRepository.findByEmailWithDetails(email)
            .orElseThrow(() -> new AppException("Utilisateur introuvable", HttpStatus.NOT_FOUND));
        return toPrivate(user);
    }

    // ── Get public profile by id ──────────────────────────

    public PublicProfileResponse getPublicProfile(String userId) {
        User user = userRepository.findByIdWithDetails(userId)
            .orElseThrow(() -> new AppException("Profil introuvable", HttpStatus.NOT_FOUND));
        if (!user.isActive()) {
            throw new AppException("Ce profil n'est pas disponible", HttpStatus.NOT_FOUND);
        }
        return toPublic(user);
    }

    // ── Update profile ────────────────────────────────────

    @Transactional
    public PrivateProfileResponse updateProfile(String email, UpdateProfileRequest req) {
        User user = userRepository.findByEmailWithDetails(email)
            .orElseThrow(() -> new AppException("Utilisateur introuvable", HttpStatus.NOT_FOUND));

        if (req.getFirstName() != null) user.setFirstName(req.getFirstName());
        if (req.getLastName() != null) user.setLastName(req.getLastName());
        if (req.getPhoneNumber() != null) user.setPhoneNumber(req.getPhoneNumber());
        if (req.getCity() != null) user.setCity(req.getCity());
        if (req.getWilaya() != null) user.setWilaya(req.getWilaya());
        if (req.getPreferredLanguage() != null) user.setPreferredLanguage(req.getPreferredLanguage());

        // Provider fields
        if (user.isProvider()) {
            if (req.getBio() != null) user.setBio(req.getBio());
            if (req.getYearsExperience() != null) user.setYearsExperience(req.getYearsExperience());
            if (req.getBusinessName() != null) user.setBusinessName(req.getBusinessName());
            if (req.getBusinessDescription() != null) user.setBusinessDescription(req.getBusinessDescription());
            if (req.getWebsiteUrl() != null) user.setWebsiteUrl(req.getWebsiteUrl());
            if (req.getSkills() != null) user.setSkills(req.getSkills());
        }

        userRepository.save(user);
        return toPrivate(user);
    }

    // ── Update categories (dynamic — no code needed) ──────

    @Transactional
    public PrivateProfileResponse updateCategories(String email, UpdateCategoriesRequest req) {
        User user = userRepository.findByEmailWithDetails(email)
            .orElseThrow(() -> new AppException("Utilisateur introuvable", HttpStatus.NOT_FOUND));

        if (!user.isProvider()) {
            throw new AppException("Seuls les prestataires peuvent choisir des catégories", HttpStatus.FORBIDDEN);
        }

        List<Category> categories = categoryRepository.findAllById(req.getCategoryIds());

        if (categories.size() != req.getCategoryIds().size()) {
            throw new AppException("Une ou plusieurs catégories sont introuvables", HttpStatus.BAD_REQUEST);
        }

        // Max 5 catégories par prestataire
        if (categories.size() > 5) {
            throw new AppException("Maximum 5 catégories autorisées par prestataire", HttpStatus.BAD_REQUEST);
        }

        user.setCategories(new HashSet<>(categories));
        userRepository.save(user);

        log.info("Catégories mises à jour pour {}: {}", email,
            categories.stream().map(Category::getName).collect(Collectors.joining(", ")));

        return toPrivate(user);
    }

    // ── Change password ───────────────────────────────────

    @Transactional
    public String changePassword(String email, ChangePasswordRequest req) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException("Utilisateur introuvable", HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new AppException("Mot de passe actuel incorrect", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        return "Mot de passe modifié avec succès";
    }

    // ── Admin: list all users ─────────────────────────────

    public Page<AdminUserRow> listAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toAdminRow);
    }

    // ── Admin: toggle active ──────────────────────────────

    @Transactional
    public String toggleUserActive(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException("Utilisateur introuvable", HttpStatus.NOT_FOUND));
        user.setActive(!user.isActive());
        userRepository.save(user);
        return user.isActive() ? "Compte activé" : "Compte suspendu";
    }

    // ── Admin: verify provider ────────────────────────────

    @Transactional
    public String verifyProvider(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException("Utilisateur introuvable", HttpStatus.NOT_FOUND));
        if (!user.isProvider()) {
            throw new AppException("Cet utilisateur n'est pas un prestataire", HttpStatus.BAD_REQUEST);
        }
        user.setVerified(true);
        userRepository.save(user);
        log.info("Prestataire vérifié par admin: {}", user.getEmail());
        return "Prestataire vérifié avec succès";
    }

    // ── Mappers ───────────────────────────────────────────

    private PublicProfileResponse toPublic(User u) {
        return PublicProfileResponse.builder()
            .id(u.getId())
            .fullName(u.getFullName())
            .avatarUrl(u.getAvatarUrl())
            .role(u.getRole())
            .providerType(u.getProviderType())
            .verified(u.isVerified())
            .providerLevel(u.getProviderLevel())
            .city(u.getCity())
            .wilaya(u.getWilaya())
            .bio(u.getBio())
            .businessName(u.getBusinessName())
            .businessDescription(u.getBusinessDescription())
            .websiteUrl(u.getWebsiteUrl())
            .yearsExperience(u.getYearsExperience())
            .averageRating(u.getAverageRating())
            .totalReviews(u.getTotalReviews())
            .completedOrders(u.getCompletedOrders())
            .skills(u.getSkills())
            .categories(u.getCategories().stream().map(this::toCategoryFlat).collect(Collectors.toList()))
            .memberSince(u.getCreatedAt())
            .build();
    }

    private PrivateProfileResponse toPrivate(User u) {
        return PrivateProfileResponse.builder()
            .id(u.getId())
            .email(u.getEmail())
            .firstName(u.getFirstName())
            .lastName(u.getLastName())
            .fullName(u.getFullName())
            .avatarUrl(u.getAvatarUrl())
            .role(u.getRole())
            .providerType(u.getProviderType())
            .emailVerified(u.isEmailVerified())
            .verified(u.isVerified())
            .active(u.isActive())
            .providerLevel(u.getProviderLevel())
            .phoneNumber(u.getPhoneNumber())
            .city(u.getCity())
            .wilaya(u.getWilaya())
            .preferredLanguage(u.getPreferredLanguage())
            .bio(u.getBio())
            .businessName(u.getBusinessName())
            .businessDescription(u.getBusinessDescription())
            .websiteUrl(u.getWebsiteUrl())
            .yearsExperience(u.getYearsExperience())
            .averageRating(u.getAverageRating())
            .totalReviews(u.getTotalReviews())
            .completedOrders(u.getCompletedOrders())
            .walletBalance(u.getWalletBalance())
            .skills(u.getSkills())
            .categories(u.getCategories().stream().map(this::toCategoryFlat).collect(Collectors.toList()))
            .createdAt(u.getCreatedAt())
            .updatedAt(u.getUpdatedAt())
            .build();
    }

    private AdminUserRow toAdminRow(User u) {
        return AdminUserRow.builder()
            .id(u.getId())
            .email(u.getEmail())
            .fullName(u.getFullName())
            .role(u.getRole())
            .providerType(u.getProviderType())
            .emailVerified(u.isEmailVerified())
            .verified(u.isVerified())
            .active(u.isActive())
            .completedOrders(u.getCompletedOrders())
            .averageRating(u.getAverageRating())
            .createdAt(u.getCreatedAt())
            .build();
    }

    private CategoryFlat toCategoryFlat(Category c) {
        return CategoryFlat.builder()
            .id(c.getId())
            .name(c.getName())
            .nameAr(c.getNameAr())
            .slug(c.getSlug())
            .iconName(c.getIconName())
            .parentId(c.getParent() != null ? c.getParent().getId() : null)
            .parentName(c.getParent() != null ? c.getParent().getName() : null)
            .level(c.getLevel())
            .build();
    }
}
