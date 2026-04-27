package classenrollmentsystem.enrollment.repository;

import classenrollmentsystem.enrollment.entity.Enrollment;
import classenrollmentsystem.enrollment.entity.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrollmentJpaRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByUserIdAndCourseIdAndStatusNot(Long userId, Long courseId, EnrollmentStatus status);

    int countByCourseIdAndStatusNot(Long courseId, EnrollmentStatus status);

    Page<Enrollment> findAllByUserId(Long userId, Pageable pageable);

}
