package dz.freelance.modules.category.service;

import dz.freelance.modules.category.dto.CategoryDtos.*;
import dz.freelance.modules.category.entity.Category;
import dz.freelance.modules.category.repository.CategoryRepository;
import dz.freelance.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // ── PUBLIC ────────────────────────────────────────────

    @Cacheable("categories-tree")
    public List<CategoryResponse> getPublicTree() {
        return categoryRepository.findRootCategoriesWithChildren()
            .stream()
            .map(this::toResponseWithChildren)
            .collect(Collectors.toList());
    }

    @Cacheable("categories-flat")
    public List<CategoryFlat> getAllFlat() {
        return categoryRepository.findByActiveTrueOrderByLevelAscSortOrderAsc()
            .stream()
            .map(this::toFlat)
            .collect(Collectors.toList());
    }

    public List<CategoryFlat> getSubCategories(Long parentId) {
        return categoryRepository.findByParentIdAndActiveTrueOrderBySortOrderAsc(parentId)
            .stream()
            .map(this::toFlat)
            .collect(Collectors.toList());
    }

    // ── ADMIN CRUD ────────────────────────────────────────

    @Transactional
    @CacheEvict(value = {"categories-tree", "categories-flat"}, allEntries = true)
    public CategoryResponse create(CreateCategoryRequest req) {
        String slug = generateSlug(req.getName(), req.getParentId());

        if (categoryRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        Category parent = null;
        int level = 0;

        if (req.getParentId() != null) {
            parent = categoryRepository.findById(req.getParentId())
                .orElseThrow(() -> new AppException("Catégorie parente introuvable", HttpStatus.NOT_FOUND));
            level = parent.getLevel() + 1;

            if (level > 2) {
                throw new AppException("Maximum 3 niveaux de catégories autorisés", HttpStatus.BAD_REQUEST);
            }
        }

        Category category = Category.builder()
            .name(req.getName())
            .nameAr(req.getNameAr())
            .slug(slug)
            .description(req.getDescription())
            .iconName(req.getIconName())
            .parent(parent)
            .level(level)
            .sortOrder(req.getSortOrder())
            .build();

        categoryRepository.save(category);
        log.info("Catégorie créée: {} (niveau {})", category.getName(), level);
        return toResponse(category);
    }

    @Transactional
    @CacheEvict(value = {"categories-tree", "categories-flat"}, allEntries = true)
    public CategoryResponse update(Long id, UpdateCategoryRequest req) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new AppException("Catégorie introuvable", HttpStatus.NOT_FOUND));

        if (req.getName() != null) category.setName(req.getName());
        if (req.getNameAr() != null) category.setNameAr(req.getNameAr());
        if (req.getDescription() != null) category.setDescription(req.getDescription());
        if (req.getIconName() != null) category.setIconName(req.getIconName());
        if (req.getSortOrder() != null) category.setSortOrder(req.getSortOrder());
        if (req.getActive() != null) category.setActive(req.getActive());

        if (req.getParentId() != null && !req.getParentId().equals(
                category.getParent() != null ? category.getParent().getId() : null)) {
            Category newParent = categoryRepository.findById(req.getParentId())
                .orElseThrow(() -> new AppException("Catégorie parente introuvable", HttpStatus.NOT_FOUND));
            category.setParent(newParent);
            category.setLevel(newParent.getLevel() + 1);
        }

        categoryRepository.save(category);
        return toResponse(category);
    }

    @Transactional
    @CacheEvict(value = {"categories-tree", "categories-flat"}, allEntries = true)
    public void toggleActive(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new AppException("Catégorie introuvable", HttpStatus.NOT_FOUND));
        category.setActive(!category.isActive());
        categoryRepository.save(category);
    }

    @Transactional
    @CacheEvict(value = {"categories-tree", "categories-flat"}, allEntries = true)
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new AppException("Catégorie introuvable", HttpStatus.NOT_FOUND));

        if (!category.getChildren().isEmpty()) {
            throw new AppException("Impossible de supprimer une catégorie avec des sous-catégories", HttpStatus.BAD_REQUEST);
        }

        categoryRepository.delete(category);
    }

    // ── Admin list (all, including inactive) ─────────────

    public List<CategoryResponse> getAllForAdmin() {
        return categoryRepository.findByParentIsNullOrderBySortOrderAsc()
            .stream()
            .map(this::toResponseWithChildren)
            .collect(Collectors.toList());
    }

    // ── Mappers ───────────────────────────────────────────

    private CategoryResponse toResponse(Category c) {
        return CategoryResponse.builder()
            .id(c.getId())
            .name(c.getName())
            .nameAr(c.getNameAr())
            .slug(c.getSlug())
            .description(c.getDescription())
            .iconName(c.getIconName())
            .parentId(c.getParent() != null ? c.getParent().getId() : null)
            .level(c.getLevel())
            .active(c.isActive())
            .sortOrder(c.getSortOrder())
            .providerCount(c.getProviderCount())
            .serviceCount(c.getServiceCount())
            .build();
    }

    private CategoryResponse toResponseWithChildren(Category c) {
        CategoryResponse resp = toResponse(c);
        if (c.getChildren() != null) {
            resp.setChildren(c.getChildren().stream()
                .filter(Category::isActive)
                .map(this::toResponseWithChildren)
                .collect(Collectors.toList()));
        }
        return resp;
    }

    private CategoryFlat toFlat(Category c) {
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

    private String generateSlug(String name, Long parentId) {
        String normalized = Normalizer.normalize(name.toLowerCase(), Normalizer.Form.NFD)
            .replaceAll("[^\\p{ASCII}]", "")
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .trim();

        if (parentId != null) {
            return categoryRepository.findById(parentId)
                .map(p -> p.getSlug() + "-" + normalized)
                .orElse(normalized);
        }
        return normalized;
    }
}
