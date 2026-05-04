package dz.freelance.modules.category.controller;

import dz.freelance.modules.category.dto.CategoryDtos.*;
import dz.freelance.modules.category.service.CategoryService;
import dz.freelance.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // ── PUBLIC ────────────────────────────────────────────

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getPublicTree() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getPublicTree()));
    }

    @GetMapping("/flat")
    public ResponseEntity<ApiResponse<List<CategoryFlat>>> getAllFlat() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllFlat()));
    }

    @GetMapping("/{parentId}/sub")
    public ResponseEntity<ApiResponse<List<CategoryFlat>>> getSubCategories(@PathVariable Long parentId) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getSubCategories(parentId)));
    }

    // ── ADMIN ─────────────────────────────────────────────

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllForAdmin() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllForAdmin()));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CreateCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(categoryService.create(req)));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(@PathVariable Long id,
                                                                 @RequestBody UpdateCategoryRequest req) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.update(id, req)));
    }

    @PatchMapping("/admin/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> toggleActive(@PathVariable Long id) {
        categoryService.toggleActive(id);
        return ResponseEntity.ok(ApiResponse.success("Statut de la catégorie mis à jour"));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Catégorie supprimée"));
    }
}
