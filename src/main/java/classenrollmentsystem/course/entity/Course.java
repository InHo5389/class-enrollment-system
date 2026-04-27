package classenrollmentsystem.course.entity;

import classenrollmentsystem.common.entity.BaseEntity;
import classenrollmentsystem.user.entity.CreatorProfile;
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

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "courses")
public class Course extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_profile_id", nullable = false)
    private CreatorProfile creatorProfile;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer maxCapacity;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseStatus status;


    public static Course create(
            CreatorProfile creatorProfile,
            String title,
            String description,
            BigDecimal price,
            int maxCapacity,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return Course.builder()
                .creatorProfile(creatorProfile)
                .title(title)
                .description(description)
                .price(price)
                .maxCapacity(maxCapacity)
                .startDate(startDate)
                .endDate(endDate)
                .status(CourseStatus.DRAFT)
                .build();
    }

    public void changeStatus(CourseStatus newStatus) {
        this.status.validateTransitTo(newStatus);
        this.status = newStatus;
    }

}
