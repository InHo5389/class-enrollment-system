package classenrollmentsystem.course.controller.dto.request;

import classenrollmentsystem.course.entity.CourseStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChangeCourseStatusRequest {

    private CourseStatus status;

    public static ChangeCourseStatusRequest from(CourseStatus status) {
        ChangeCourseStatusRequest request = new ChangeCourseStatusRequest();
        request.status = status;
        return request;
    }

}
