package classenrollmentsystem.course.service;

import classenrollmentsystem.course.entity.Course;
import classenrollmentsystem.course.entity.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CourseRepository {

    Course save(Course course);

    Optional<Course> findById(Long id);

    Page<Course> findAllByStatus(CourseStatus status, Pageable pageable);

    Page<Course> findAll(Pageable pageable);

    void delete(Course course);

}
