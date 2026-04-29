package classenrollmentsystem.course.service;

import classenrollmentsystem.course.service.dto.CourseDetailDto;

import java.util.Optional;

public interface CourseCacheRepository {

    Optional<CourseDetailDto> findById(Long courseId);

    void save(CourseDetailDto dto);

    void evict(Long courseId);

}
