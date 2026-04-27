package classenrollmentsystem.enrollment.service;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import classenrollmentsystem.course.entity.Course;
import classenrollmentsystem.course.entity.CourseStatus;
import classenrollmentsystem.course.service.CourseRepository;
import classenrollmentsystem.enrollment.entity.Enrollment;
import classenrollmentsystem.enrollment.entity.EnrollmentStatus;
import classenrollmentsystem.enrollment.service.dto.EnrollmentDto;
import classenrollmentsystem.user.entity.User;
import classenrollmentsystem.user.service.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentCountRepository enrollmentCountRepository;

    @Value("${enrollment.cancel-deadline-days}")
    private int cancelDeadlineDays;

    @Transactional
    public EnrollmentDto enroll(Long userId, Long courseId) {
        log.info("수강 신청 시작 - 사용자 ID: {}, 강의 ID: {}", userId, courseId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 사용자의 수강 신청 시도 - 사용자 ID: {}", userId);
                    return new CustomGlobalException(ErrorType.USER_NOT_FOUND);
                });

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 강의에 수강 신청 시도 - 강의 ID: {}", courseId);
                    return new CustomGlobalException(ErrorType.COURSE_NOT_FOUND);
                });

        if (course.getCreatorProfile().getUser().getId().equals(userId)) {
            log.warn("본인 강의 수강 신청 시도 - 사용자 ID: {}, 강의 ID: {}", userId, courseId);
            throw new CustomGlobalException(ErrorType.ENROLLMENT_OWN_COURSE);
        }

        if (course.getStatus() != CourseStatus.OPEN) {
            log.warn("모집 중이 아닌 강의에 수강 신청 시도 - 강의 ID: {}, 상태: {}", courseId, course.getStatus());
            throw new CustomGlobalException(ErrorType.ENROLLMENT_COURSE_NOT_OPEN);
        }

        if (enrollmentRepository.existsByUserIdAndCourseIdAndStatusNot(userId, courseId, EnrollmentStatus.CANCELLED)) {
            log.warn("중복 수강 신청 시도 - 사용자 ID: {}, 강의 ID: {}", userId, courseId);
            throw new CustomGlobalException(ErrorType.ENROLLMENT_ALREADY_EXISTS);
        }

        long countAfterIncrement = enrollmentCountRepository.incrementEnrollmentCount(courseId);
        if (countAfterIncrement > course.getMaxCapacity()) {
            enrollmentCountRepository.decrementEnrollmentCount(courseId);
            log.warn("정원 초과로 수강 신청 거부 - 강의 ID: {}, Redis 카운트: {}, 정원: {}", courseId, countAfterIncrement, course.getMaxCapacity());
            throw new CustomGlobalException(ErrorType.ENROLLMENT_CAPACITY_EXCEEDED);
        }

        Enrollment enrollment = Enrollment.create(user, course);
        Enrollment saved = enrollmentRepository.save(enrollment);

        log.info("수강 신청 완료 - 수강 신청 ID: {}, 사용자 ID: {}, 강의 ID: {}", saved.getId(), userId, courseId);
        return EnrollmentDto.from(saved);
    }

    @Transactional
    public EnrollmentDto confirm(Long enrollmentId, Long userId) {
        log.info("결제 확정 시작 - 수강 신청 ID: {}, 사용자 ID: {}", enrollmentId, userId);

        Enrollment enrollment = findEnrollmentByOwner(enrollmentId, userId);
        enrollment.confirm();
        Enrollment saved = enrollmentRepository.save(enrollment);

        log.info("결제 확정 완료 - 수강 신청 ID: {}", saved.getId());
        return EnrollmentDto.from(saved);
    }

    @Transactional
    public EnrollmentDto cancel(Long enrollmentId, Long userId) {
        return cancel(enrollmentId, userId, LocalDateTime.now());
    }

    @Transactional
    public EnrollmentDto cancel(Long enrollmentId, Long userId, LocalDateTime currentDateTime) {
        log.info("수강 취소 시작 - 수강 신청 ID: {}, 사용자 ID: {}", enrollmentId, userId);

        Enrollment enrollment = findEnrollmentByOwner(enrollmentId, userId);

        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            log.warn("이미 취소된 수강 신청 취소 시도 - 수강 신청 ID: {}", enrollmentId);
            throw new CustomGlobalException(ErrorType.ENROLLMENT_CANCEL_NOT_ALLOWED);
        }

        if (enrollment.getStatus() == EnrollmentStatus.CONFIRMED) {
            validateCancelDeadline(enrollment, currentDateTime);
        }

        enrollment.cancel();
        Enrollment saved = enrollmentRepository.save(enrollment);

        enrollmentCountRepository.decrementEnrollmentCount(enrollment.getCourse().getId());

        log.info("수강 취소 완료 - 수강 신청 ID: {}", saved.getId());
        return EnrollmentDto.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentDto> getMyEnrollments(Long userId, Pageable pageable) {
        log.debug("내 수강 신청 목록 조회 - 사용자 ID: {}", userId);
        return enrollmentRepository.findAllByUserId(userId, pageable).map(EnrollmentDto::from);
    }

    private void validateCancelDeadline(Enrollment enrollment, LocalDateTime currentDateTime) {
        LocalDateTime confirmedAt = enrollment.getConfirmedAt();
        LocalDateTime deadline = confirmedAt.plusDays(cancelDeadlineDays);

        if (currentDateTime.isAfter(deadline)) {
            log.warn("취소 기간 초과 - 수강 신청 ID: {}, 결제 확정일: {}, 취소 마감일: {}",
                    enrollment.getId(), confirmedAt, deadline);
            throw new CustomGlobalException(ErrorType.ENROLLMENT_CANCEL_DEADLINE_EXCEEDED);
        }
    }

    private Enrollment findEnrollmentByOwner(Long enrollmentId, Long userId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 수강 신청 접근 - 수강 신청 ID: {}", enrollmentId);
                    return new CustomGlobalException(ErrorType.ENROLLMENT_NOT_FOUND);
                });

        if (!enrollment.getUser().getId().equals(userId)) {
            log.warn("수강 신청 소유자가 아닌 사용자 접근 - 수강 신청 ID: {}, 요청자 ID: {}", enrollmentId, userId);
            throw new CustomGlobalException(ErrorType.ENROLLMENT_NOT_OWNER);
        }

        return enrollment;
    }

}
