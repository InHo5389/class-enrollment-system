package classenrollmentsystem.user.repository;

import classenrollmentsystem.user.entity.CreatorProfile;
import classenrollmentsystem.user.service.CreatorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CreatorProfileRepositoryImpl implements CreatorProfileRepository {

    private final CreatorProfileJpaRepository creatorProfileJpaRepository;

    @Override
    public CreatorProfile save(CreatorProfile creatorProfile) {
        return creatorProfileJpaRepository.save(creatorProfile);
    }

    @Override
    public Optional<CreatorProfile> findById(Long id) {
        return creatorProfileJpaRepository.findById(id);
    }

    @Override
    public Optional<CreatorProfile> findByUserId(Long userId) {
        return creatorProfileJpaRepository.findByUserId(userId);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return creatorProfileJpaRepository.existsByUserId(userId);
    }

    @Override
    public void delete(CreatorProfile creatorProfile) {
        creatorProfileJpaRepository.delete(creatorProfile);
    }

}
