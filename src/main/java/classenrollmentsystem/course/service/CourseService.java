package classenrollmentsystem.course.service;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import classenrollmentsystem.course.entity.Course;
import classenrollmentsystem.course.entity.CourseStatus;
import classenrollmentsystem.course.service.dto.CourseDetailDto;
import classenrollmentsystem.course.service.dto.CourseDto;
import classenrollmentsystem.course.service.dto.CreateCourseDto;
import classenrollmentsystem.user.entity.CreatorProfile;
import classenrollmentsystem.user.service.CreatorProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CreatorProfileRepository creatorProfileRepository;

    @Transactional
    public CourseDto createCourse(CreateCourseDto dto) {
        log.info("강의 등록 시작 - 사용자 ID: {}", dto.getUserId());

        validateCourseInput(dto);

        CreatorProfile creatorProfile = creatorProfileRepository.findByUserId(dto.getUserId())
                .orElseThrow(() -> {
                    log.warn("크리에이터 권한 없는 사용자의 강의 등록 시도 - 사용자 ID: {}", dto.getUserId());
                    return new CustomGlobalException(ErrorType.NOT_CREATOR);
                });

        Course course = Course.create(
                creatorProfile,
                dto.getTitle(),
                dto.getDescription(),
                dto.getPrice(),
                dto.getMaxCapacity(),
                dto.getStartDate(),
                dto.getEndDate()
        );

        Course saved = courseRepository.save(course);
        log.info("강의 등록 완료 - 강의 ID: {}, 제목: {}", saved.getId(), saved.getTitle());

        return CourseDto.of(saved, 0);
    }

    @Transactional(readOnly = true)
    public Page<CourseDto> getCourses(CourseStatus status, Pageable pageable) {
        log.debug("강의 목록 조회 - 상태 필터: {}", status);

        Page<Course> courses = (status != null)
                ? courseRepository.findAllByStatus(status, pageable)
                : courseRepository.findAll(pageable);

        return courses.map(course -> CourseDto.of(course, 0));
    }

    @Transactional(readOnly = true)
    public CourseDetailDto getCourse(Long courseId, Long userId) {
        log.debug("강의 상세 조회 - 강의 ID: {}", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 강의 조회 - 강의 ID: {}", courseId);
                    return new CustomGlobalException(ErrorType.COURSE_NOT_FOUND);
                });

        return CourseDetailDto.of(course, 0);
    }

    @Transactional
    public CourseDto changeCourseStatus(Long courseId, Long userId, CourseStatus newStatus) {
        log.info("강의 상태 변경 시작 - 강의 ID: {}, 요청자 ID: {}, 새 상태: {}", courseId, userId, newStatus);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 강의 상태 변경 시도 - 강의 ID: {}", courseId);
                    return new CustomGlobalException(ErrorType.COURSE_NOT_FOUND);
                });

        Long ownerUserId = course.getCreatorProfile().getUser().getId();
        if (!ownerUserId.equals(userId)) {
            log.warn("강의 소유자가 아닌 사용자의 상태 변경 시도 - 강의 ID: {}, 요청자 ID: {}", courseId, userId);
            throw new CustomGlobalException(ErrorType.COURSE_NOT_OWNER);
        }

        course.changeStatus(newStatus);
        Course saved = courseRepository.save(course);

        log.info("강의 상태 변경 완료 - 강의 ID: {}, 상태: {}", saved.getId(), saved.getStatus());
        return CourseDto.of(saved, 0);
    }

    private void validateCourseInput(CreateCourseDto dto) {
        if (dto.getMaxCapacity() < 1) {
            throw new CustomGlobalException(ErrorType.INVALID_COURSE_CAPACITY);
        }
        if (!dto.getEndDate().isAfter(dto.getStartDate())) {
            throw new CustomGlobalException(ErrorType.INVALID_COURSE_PERIOD);
        }
    }

}
