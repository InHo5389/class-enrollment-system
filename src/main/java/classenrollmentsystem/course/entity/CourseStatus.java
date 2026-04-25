package classenrollmentsystem.course.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseStatus {
    DRAFT("초안"),
    OPEN("모집 중"),
    CLOSED("모집 마감");

    private final String text;
}
