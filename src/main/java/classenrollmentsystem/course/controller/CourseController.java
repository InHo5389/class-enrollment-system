package classenrollmentsystem.course.controller;

import classenrollmentsystem.common.dto.PageResponse;
import classenrollmentsystem.common.interceptor.annotation.PublicApi;
import classenrollmentsystem.common.interceptor.annotation.RequireCreator;
import classenrollmentsystem.course.controller.dto.request.ChangeCourseStatusRequest;
import classenrollmentsystem.course.controller.dto.request.CreateCourseRequest;
import classenrollmentsystem.course.controller.dto.response.CourseDetailResponse;
import classenrollmentsystem.course.controller.dto.response.CourseResponse;
import classenrollmentsystem.course.entity.CourseStatus;
import classenrollmentsystem.course.service.CourseService;
import classenrollmentsystem.course.service.dto.CourseDetailDto;
import classenrollmentsystem.course.service.dto.CourseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @RequireCreator
    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody CreateCourseRequest request
    ) {
        log.debug("강의 등록 요청 - 사용자 ID: {}", userId);
        CourseDto courseDto = courseService.createCourse(request.toDto(userId));

        return ResponseEntity.ok(CourseResponse.from(courseDto));
    }

    @PublicApi
    @GetMapping
    public ResponseEntity<PageResponse<CourseResponse>> getCourses(
            @RequestParam(required = false, defaultValue = "OPEN") CourseStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.debug("강의 목록 조회 요청 - 상태 필터: {}", status);
        Page<CourseDto> courseDtos = courseService.getCourses(status, pageable);

        return ResponseEntity.ok(PageResponse.from(courseDtos.map(CourseResponse::from)));
    }

    @PublicApi
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDetailResponse> getCourse(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long courseId
    ) {
        log.debug("강의 상세 조회 요청 - 강의 ID: {}, 사용자 ID: {}", courseId, userId);
        CourseDetailDto courseDetailDto = courseService.getCourse(courseId, userId);

        return ResponseEntity.ok(CourseDetailResponse.from(courseDetailDto));
    }

    @PatchMapping("/{courseId}/status")
    public ResponseEntity<CourseResponse> changeCourseStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long courseId,
            @RequestBody ChangeCourseStatusRequest request
    ) {
        log.debug("강의 상태 변경 요청 - 강의 ID: {}, 사용자 ID: {}, 새 상태: {}", courseId, userId, request.getStatus());
        CourseDto courseDto = courseService.changeCourseStatus(courseId, userId, request.getStatus());

        return ResponseEntity.ok(CourseResponse.from(courseDto));
    }

}
