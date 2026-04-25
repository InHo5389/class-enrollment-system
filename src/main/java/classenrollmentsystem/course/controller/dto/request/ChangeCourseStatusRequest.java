package classenrollmentsystem.course.controller.dto.request;

import classenrollmentsystem.course.entity.CourseStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChangeCourseStatusRequest {

    private CourseStatus status;

}
