package classenrollmentsystem.course.repository;

import classenrollmentsystem.course.entity.Course;
import classenrollmentsystem.course.entity.CourseStatus;
import classenrollmentsystem.course.service.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CourseRepositoryImpl implements CourseRepository {

    private final CourseJpaRepository courseJpaRepository;

    @Override
    public Course save(Course course) {
        return courseJpaRepository.save(course);
    }

    @Override
    public Optional<Course> findById(Long id) {
        return courseJpaRepository.findById(id);
    }

    @Override
    public Page<Course> findAllByStatus(CourseStatus status, Pageable pageable) {
        return courseJpaRepository.findAllByStatus(status, pageable);
    }

    @Override
    public Page<Course> findAll(Pageable pageable) {
        return courseJpaRepository.findAll(pageable);
    }

    @Override
    public void delete(Course course) {
        courseJpaRepository.delete(course);
    }

}
