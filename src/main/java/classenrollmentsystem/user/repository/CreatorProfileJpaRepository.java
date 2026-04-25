package classenrollmentsystem.user.repository;

import classenrollmentsystem.user.entity.CreatorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreatorProfileJpaRepository extends JpaRepository<CreatorProfile, Long> {

    Optional<CreatorProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

}
