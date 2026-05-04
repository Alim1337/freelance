package dz.freelance.modules.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role; // CLIENT, PROVIDER, ADMIN

    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // ── Champs communs ──────────────────────────────────
    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "avatar_url")
    private String avatarUrl;

    private String city;
    private String wilaya;

    @Column(name = "preferred_language")
    @Builder.Default
    private String preferredLanguage = "fr";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Profil CLIENT ────────────────────────────────────
    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    // ── Profil PROVIDER ─────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type")
    private ProviderType providerType; // PERSON, ORGANISM (null si CLIENT)

    // PERSON
    private String bio;

    @Column(name = "years_experience")
    private Integer yearsExperience;

    // ORGANISM
    @Column(name = "business_name")
    private String businessName;

    @Column(name = "business_description", columnDefinition = "TEXT")
    private String businessDescription;

    @Column(name = "website_url")
    private String websiteUrl;

    // ── Vérification & niveaux ──────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_level")
    @Builder.Default
    private ProviderLevel providerLevel = ProviderLevel.NEW; // NEW, RISING, TOP, EXPERT

    @Column(name = "completed_orders")
    @Builder.Default
    private int completedOrders = 0;

    @Column(name = "average_rating")
    @Builder.Default
    private double averageRating = 0.0;

    @Column(name = "total_reviews")
    @Builder.Default
    private int totalReviews = 0;

    // ── Catégories choisies (dynamiques) ────────────────
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_categories",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private Set<dz.freelance.modules.category.entity.Category> categories = new HashSet<>();

    // ── Skills (tags libres) ─────────────────────────────
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_skills", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "skill")
    @Builder.Default
    private Set<String> skills = new HashSet<>();

    // ── Wallet ────────────────────────────────────────────
    @Column(name = "wallet_balance")
    @Builder.Default
    private double walletBalance = 0.0;

    // ── Helper methods ───────────────────────────────────
    public String getFullName() {
        if (providerType == ProviderType.ORGANISM && businessName != null) {
            return businessName;
        }
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return email.split("@")[0];
    }

    public boolean isProvider() {
        return role == UserRole.PROVIDER;
    }

    public boolean isClient() {
        return role == UserRole.CLIENT;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public enum UserRole {
        CLIENT, PROVIDER, ADMIN
    }

    public enum ProviderType {
        PERSON, ORGANISM
    }

    public enum ProviderLevel {
        NEW, RISING, TOP, EXPERT
    }
}
