package it.univ.lifeplanner.user.repository;

import it.univ.lifeplanner.user.model.AppUser;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    @Query("""
        select user from AppUser user
        where lower(user.username) like lower(concat('%', :query, '%'))
           or lower(user.email) like lower(concat('%', :query, '%'))
        order by user.username asc
        """)
    List<AppUser> search(@Param("query") String query);
}
