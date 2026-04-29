package classenrollmentsystem.course.repository;

import classenrollmentsystem.course.service.CourseCacheRepository;
import classenrollmentsystem.course.service.dto.CourseDetailDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CourseCacheRepositoryImpl implements CourseCacheRepository {

    private static final String CACHE_KEY_PREFIX = "course:detail:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${course.cache.ttl-seconds}")
    private long ttlSeconds;

    @Override
    public Optional<CourseDetailDto> findById(Long courseId) {
        String key = buildKey(courseId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, CourseDetailDto.class));
        } catch (JsonProcessingException e) {
            log.warn("강의 캐시 역직렬화 실패 - courseId: {}", courseId, e);
            redisTemplate.delete(key);
            return Optional.empty();
        }
    }

    @Override
    public void save(CourseDetailDto dto) {
        String key = buildKey(dto.getId());
        try {
            String json = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
        } catch (JsonProcessingException e) {
            log.warn("강의 캐시 직렬화 실패 - courseId: {}", dto.getId(), e);
        }
    }

    @Override
    public void evict(Long courseId) {
        redisTemplate.delete(buildKey(courseId));
        log.debug("강의 캐시 무효화 - courseId: {}", courseId);
    }

    private String buildKey(Long courseId) {
        return CACHE_KEY_PREFIX + courseId;
    }

}
