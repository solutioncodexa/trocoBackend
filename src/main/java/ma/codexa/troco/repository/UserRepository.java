package ma.codexa.troco.repository;

import ma.codexa.troco.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(String role);

    List<User> findByFournisseurIdAndRoleIgnoreCase(Long fournisseurId, String role);

    /** Membres boutique (ADMIN/STAFF) — jamais SUPER_ADMIN ni CUSTOMER. */
    @Query("""
            SELECT u FROM User u
            WHERE u.fournisseurId = :fid
              AND UPPER(u.role) <> 'CUSTOMER'
              AND UPPER(u.role) <> 'SUPER_ADMIN'
            ORDER BY u.createdAt DESC
            """)
    List<User> findStoreMembers(@Param("fid") Long fournisseurId);

    @Query("""
            SELECT COUNT(u) FROM User u
            WHERE u.fournisseurId = :fid
              AND UPPER(u.role) <> 'CUSTOMER'
              AND UPPER(u.role) <> 'SUPER_ADMIN'
            """)
    long countStoreMembers(@Param("fid") Long fournisseurId);

    @Query("""
            SELECT COUNT(u) FROM User u
            WHERE u.fournisseurId = :fid
              AND UPPER(u.role) = 'ADMIN'
              AND u.active = true
            """)
    long countActiveAdmins(@Param("fid") Long fournisseurId);
}
