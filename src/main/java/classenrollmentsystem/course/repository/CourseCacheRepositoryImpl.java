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
    private static final String NULL_CACHE_KEY_PREFIX = "course:null:";
    private static final String LOCK_KEY_PREFIX = "course:lock:";
    private static final String NULL_SENTINEL = "__NULL__";
    private static final long LOCK_TIMEOUT_SECONDS = 3;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${course.cache.ttl-seconds}")
    private long ttlSeconds;

    @Value("${course.cache.null-ttl-seconds}")
    private long nullTtlSeconds;

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
    public boolean isNullCached(Long courseId) {
        String value = redisTemplate.opsForValue().get(buildNullKey(courseId));
        return NULL_SENTINEL.equals(value);
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
    public void saveNull(Long courseId) {
        redisTemplate.opsForValue().set(buildNullKey(courseId), NULL_SENTINEL, Duration.ofSeconds(nullTtlSeconds));
        log.debug("강의 null 캐시 저장 - courseId: {}", courseId);
    }

    @Override
    public void evict(Long courseId) {
        redisTemplate.delete(buildKey(courseId));
        redisTemplate.delete(buildNullKey(courseId));
        log.debug("강의 캐시 무효화 - courseId: {}", courseId);
    }

    public boolean tryLock(Long courseId) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(buildLockKey(courseId), "1", Duration.ofSeconds(LOCK_TIMEOUT_SECONDS));
        return Boolean.TRUE.equals(acquired);
    }

    public void releaseLock(Long courseId) {
        redisTemplate.delete(buildLockKey(courseId));
    }

    private String buildKey(Long courseId) {
        return CACHE_KEY_PREFIX + courseId;
    }

    private String buildNullKey(Long courseId) {
        return NULL_CACHE_KEY_PREFIX + courseId;
    }

    private String buildLockKey(Long courseId) {
        return LOCK_KEY_PREFIX + courseId;
    }

}
