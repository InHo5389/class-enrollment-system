package classenrollmentsystem.course.service;

import classenrollmentsystem.course.service.dto.CourseDetailDto;

import java.util.Optional;

public interface CourseCacheRepository {

    Optional<CourseDetailDto> findById(Long courseId);

    boolean isNullCached(Long courseId);

    void save(CourseDetailDto dto);

    void saveNull(Long courseId);

    void evict(Long courseId);

    boolean tryLock(Long courseId);

    void releaseLock(Long courseId);

}
