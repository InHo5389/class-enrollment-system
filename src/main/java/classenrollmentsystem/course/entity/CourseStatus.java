package classenrollmentsystem.course.entity;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum CourseStatus {
    DRAFT("초안"),
    OPEN("모집 중"),
    CLOSED("모집 마감");

    private final String text;

    private static final Map<CourseStatus, Set<CourseStatus>> TRANSITIONS = Map.of(
        DRAFT,  Set.of(OPEN),
        OPEN,   Set.of(CLOSED),
        CLOSED, Set.of()
    );

    public void validateTransitTo(CourseStatus next) {
        if (!TRANSITIONS.get(this).contains(next)) {
            throw new CustomGlobalException(ErrorType.INVALID_COURSE_STATUS_TRANSITION);
        }
    }
}
