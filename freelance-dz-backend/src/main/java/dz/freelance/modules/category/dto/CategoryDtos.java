package dz.freelance.modules.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

public class CategoryDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryResponse {
        private Long id;
        private String name;
        private String nameAr;
        private String slug;
        private String description;
        private String iconName;
        private Long parentId;
        private int level;
        private boolean active;
        private int sortOrder;
        private int providerCount;
        private int serviceCount;
        private List<CategoryResponse> children;
    }

    // Flat version for multi-select dropdowns
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryFlat {
        private Long id;
        private String name;
        private String nameAr;
        private String slug;
        private String iconName;
        private Long parentId;
        private String parentName;
        private int level;
    }

    // ── Admin create/update ───────────────────────────────

    @Data
    public static class CreateCategoryRequest {
        @NotBlank(message = "Le nom est obligatoire")
        private String name;

        private String nameAr;
        private String description;
        private String iconName;
        private Long parentId; // null = catégorie racine
        private int sortOrder;
    }

    @Data
    public static class UpdateCategoryRequest {
        private String name;
        private String nameAr;
        private String description;
        private String iconName;
        private Long parentId;
        private Integer sortOrder;
        private Boolean active;
    }

    // ── User category update ──────────────────────────────

    @Data
    public static class UpdateUserCategoriesRequest {
        @NotNull
        private List<Long> categoryIds;
    }
}
