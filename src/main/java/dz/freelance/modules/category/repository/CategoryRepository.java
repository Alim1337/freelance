package dz.freelance.modules.category.repository;

import dz.freelance.modules.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Toutes les catégories racines actives (pour affichage public)
    List<Category> findByParentIsNullAndActiveTrueOrderBySortOrderAsc();

    // Toutes les catégories racines (pour admin)
    List<Category> findByParentIsNullOrderBySortOrderAsc();

    // Enfants directs d'une catégorie
    List<Category> findByParentIdAndActiveTrueOrderBySortOrderAsc(Long parentId);

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // Arbre complet avec enfants chargés
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parent IS NULL AND c.active = true ORDER BY c.sortOrder")
    List<Category> findRootCategoriesWithChildren();

    // Toutes les catégories actives à plat (pour select/multi-select dans le frontend)
    List<Category> findByActiveTrueOrderByLevelAscSortOrderAsc();
}
