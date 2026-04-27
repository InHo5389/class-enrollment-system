package classenrollmentsystem.enrollment.service;

public interface EnrollmentCountRepository {

    long incrementEnrollmentCount(Long courseId);

    void decrementEnrollmentCount(Long courseId);

    int getEnrollmentCount(Long courseId);

}
