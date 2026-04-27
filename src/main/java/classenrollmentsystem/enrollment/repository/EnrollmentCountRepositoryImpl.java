package classenrollmentsystem.enrollment.repository;

import classenrollmentsystem.enrollment.service.EnrollmentCountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EnrollmentCountRepositoryImpl implements EnrollmentCountRepository {

    private static final String ENROLLMENT_COUNT_KEY = "enrollment:count:";

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public long incrementEnrollmentCount(Long courseId) {
        Long count = redisTemplate.opsForValue().increment(ENROLLMENT_COUNT_KEY + courseId);
        return count == null ? 0 : count;
    }

    @Override
    public void decrementEnrollmentCount(Long courseId) {
        redisTemplate.opsForValue().decrement(ENROLLMENT_COUNT_KEY + courseId);
    }

    @Override
    public int getEnrollmentCount(Long courseId) {
        String value = redisTemplate.opsForValue().get(ENROLLMENT_COUNT_KEY + courseId);
        return value == null ? 0 : Integer.parseInt(value);
    }

}
