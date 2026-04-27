package classenrollmentsystem.enrollment.entity;

import classenrollmentsystem.common.entity.BaseEntity;
import classenrollmentsystem.course.entity.Course;
import classenrollmentsystem.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "enrollments")
public class Enrollment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status;

    @Column
    private LocalDateTime confirmedAt;

    public static Enrollment create(User user, Course course) {
        return Enrollment.builder()
                .user(user)
                .course(course)
                .status(EnrollmentStatus.PENDING)
                .build();
    }

    public void confirm() {
        this.status.validateTransitTo(EnrollmentStatus.CONFIRMED);
        this.status = EnrollmentStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status.validateTransitTo(EnrollmentStatus.CANCELLED);
        this.status = EnrollmentStatus.CANCELLED;
    }

}
