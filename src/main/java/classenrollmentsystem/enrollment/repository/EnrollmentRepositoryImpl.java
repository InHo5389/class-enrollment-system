package classenrollmentsystem.enrollment.repository;

import classenrollmentsystem.enrollment.entity.Enrollment;
import classenrollmentsystem.enrollment.entity.EnrollmentStatus;
import classenrollmentsystem.enrollment.service.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EnrollmentRepositoryImpl implements EnrollmentRepository {

    private final EnrollmentJpaRepository enrollmentJpaRepository;

    @Override
    public Enrollment save(Enrollment enrollment) {
        return enrollmentJpaRepository.save(enrollment);
    }

    @Override
    public Optional<Enrollment> findById(Long id) {
        return enrollmentJpaRepository.findById(id);
    }

    @Override
    public boolean existsByUserIdAndCourseIdAndStatusNot(Long userId, Long courseId, EnrollmentStatus status) {
        return enrollmentJpaRepository.existsByUserIdAndCourseIdAndStatusNot(userId, courseId, status);
    }

    @Override
    public int countByCourseIdAndStatusNot(Long courseId, EnrollmentStatus status) {
        return enrollmentJpaRepository.countByCourseIdAndStatusNot(courseId, status);
    }

    @Override
    public Page<Enrollment> findAllByUserId(Long userId, Pageable pageable) {
        return enrollmentJpaRepository.findAllByUserId(userId, pageable);
    }

}
