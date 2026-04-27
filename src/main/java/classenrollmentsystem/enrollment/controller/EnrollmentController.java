package classenrollmentsystem.enrollment.controller;

import classenrollmentsystem.common.dto.PageResponse;
import classenrollmentsystem.enrollment.controller.dto.request.EnrollRequest;
import classenrollmentsystem.enrollment.controller.dto.response.EnrollmentResponse;
import classenrollmentsystem.enrollment.service.EnrollmentService;
import classenrollmentsystem.enrollment.service.dto.EnrollmentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    public ResponseEntity<EnrollmentResponse> enroll(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody EnrollRequest request
    ) {
        log.debug("수강 신청 요청 - 사용자 ID: {}, 강의 ID: {}", userId, request.getCourseId());
        EnrollmentDto dto = enrollmentService.enroll(userId, request.getCourseId());
        return ResponseEntity.ok(EnrollmentResponse.from(dto));
    }

    @PostMapping("/{enrollmentId}/confirm")
    public ResponseEntity<EnrollmentResponse> confirm(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long enrollmentId
    ) {
        log.debug("결제 확정 요청 - 수강 신청 ID: {}, 사용자 ID: {}", enrollmentId, userId);
        EnrollmentDto dto = enrollmentService.confirm(enrollmentId, userId);
        return ResponseEntity.ok(EnrollmentResponse.from(dto));
    }

    @DeleteMapping("/{enrollmentId}")
    public ResponseEntity<EnrollmentResponse> cancel(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long enrollmentId
    ) {
        log.debug("수강 취소 요청 - 수강 신청 ID: {}, 사용자 ID: {}", enrollmentId, userId);
        EnrollmentDto dto = enrollmentService.cancel(enrollmentId, userId);
        return ResponseEntity.ok(EnrollmentResponse.from(dto));
    }

    @GetMapping("/me")
    public ResponseEntity<PageResponse<EnrollmentResponse>> getMyEnrollments(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.debug("내 수강 신청 목록 조회 요청 - 사용자 ID: {}", userId);
        Page<EnrollmentDto> dtos = enrollmentService.getMyEnrollments(userId, pageable);
        return ResponseEntity.ok(PageResponse.from(dtos.map(EnrollmentResponse::from)));
    }

}
