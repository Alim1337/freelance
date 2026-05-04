package dz.freelance.modules.user.repository;

import dz.freelance.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.categories LEFT JOIN FETCH u.skills WHERE u.id = :id")
    Optional<User> findByIdWithDetails(String id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.categories LEFT JOIN FETCH u.skills WHERE u.email = :email")
    Optional<User> findByEmailWithDetails(String email);
}
