package dz.freelance.modules.category.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_ar")
    private String nameAr; // Traduction arabe

    @Column(unique = true, nullable = false)
    private String slug; // Ex: "informatique-dev-web"

    private String description;

    @Column(name = "icon_name")
    private String iconName; // Nom d'icône Lucide (ex: "Code2", "Palette")

    // ── Arbre auto-référencé ─────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent; // null = catégorie racine

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Category> children = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private int level = 0; // 0 = racine, 1 = sous-catégorie, 2 = spécialité

    // ── Admin controls ───────────────────────────────────
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    // ── Stats (mise à jour asynchrone) ──────────────────
    @Column(name = "provider_count")
    @Builder.Default
    private int providerCount = 0;

    @Column(name = "service_count")
    @Builder.Default
    private int serviceCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Helper ───────────────────────────────────────────
    public boolean isRoot() {
        return parent == null;
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
}
