package classenrollmentsystem.enrollment.entity;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum EnrollmentStatus {
    PENDING("결제 대기"),
    CONFIRMED("결제 완료"),
    CANCELLED("취소됨");

    private final String description;

    private static final Map<EnrollmentStatus, Set<EnrollmentStatus>> TRANSITIONS = Map.of(
        PENDING,   Set.of(CONFIRMED, CANCELLED),
        CONFIRMED, Set.of(CANCELLED),
        CANCELLED, Set.of()
    );

    public boolean canTransitTo(EnrollmentStatus next) {
        return TRANSITIONS.get(this).contains(next);
    }

    public void validateTransitTo(EnrollmentStatus next) {
        if (!canTransitTo(next)) {
            throw new CustomGlobalException(ErrorType.ENROLLMENT_INVALID_STATUS_TRANSITION);
        }
    }
}
