package classenrollmentsystem.user.service;

import classenrollmentsystem.user.entity.CreatorProfile;

import java.util.Optional;

public interface CreatorProfileRepository {

    CreatorProfile save(CreatorProfile creatorProfile);

    Optional<CreatorProfile> findById(Long id);

    Optional<CreatorProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    void delete(CreatorProfile creatorProfile);

}
