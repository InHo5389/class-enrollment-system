package classenrollmentsystem.enrollment.repository;

import classenrollmentsystem.enrollment.service.WaitingQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class WaitingQueueRepositoryImpl implements WaitingQueueRepository {

    private static final String WAITING_KEY_PREFIX = "queue:waiting:course:";
    private static final String ACTIVE_KEY_PREFIX = "queue:active:course:";

    private static final String PROMOTE_SCRIPT = """
            local waiting_key = KEYS[1]
            local active_key = KEYS[2]
            local count = tonumber(ARGV[1])
            local expired_at = tonumber(ARGV[2])

            local members = redis.call('ZRANGE', waiting_key, 0, count - 1)
            if #members == 0 then
                return 0
            end

            for _, member in ipairs(members) do
                redis.call('ZADD', active_key, expired_at, member)
                redis.call('ZREM', waiting_key, member)
            end

            return #members
            """;

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public Set<Long> scanWaitingCourseIds() {
        Set<Long> courseIds = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(WAITING_KEY_PREFIX + "*")
                .count(100)
                .build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            cursor.forEachRemaining(key -> {
                try {
                    courseIds.add(Long.parseLong(key.replace(WAITING_KEY_PREFIX, "")));
                } catch (NumberFormatException ignored) {
                }
            });
        }
        return courseIds;
    }

    @Override
    public void addToWaitingQueue(Long courseId, Long userId, long timestamp) {
        redisTemplate.opsForZSet().add(waitingKey(courseId), userId.toString(), timestamp);
    }

    @Override
    public Long getWaitingRank(Long courseId, Long userId) {
        return redisTemplate.opsForZSet().rank(waitingKey(courseId), userId.toString());
    }

    @Override
    public Long getWaitingQueueSize(Long courseId) {
        Long size = redisTemplate.opsForZSet().size(waitingKey(courseId));
        return size == null ? 0L : size;
    }

    @Override
    public void addToActiveQueue(Long courseId, Long userId, long expiredAt) {
        redisTemplate.opsForZSet().add(activeKey(courseId), userId.toString(), expiredAt);
    }

    @Override
    public boolean isInActiveQueue(Long courseId, Long userId) {
        Double score = redisTemplate.opsForZSet().score(activeKey(courseId), userId.toString());
        if (score == null) {
            return false;
        }
        return score > System.currentTimeMillis();
    }

    @Override
    public Long getActiveQueueSize(Long courseId) {
        long now = System.currentTimeMillis();
        Long size = redisTemplate.opsForZSet().count(activeKey(courseId), now, Double.MAX_VALUE);
        return size == null ? 0L : size;
    }

    @Override
    public void removeFromActiveQueue(Long courseId, Long userId) {
        redisTemplate.opsForZSet().remove(activeKey(courseId), userId.toString());
    }

    @Override
    public void removeExpiredFromActiveQueue(Long courseId, long currentTime) {
        redisTemplate.opsForZSet().removeRangeByScore(activeKey(courseId), 0, currentTime);
    }

    @Override
    public long promoteFromWaiting(Long courseId, int count, long expiredAt) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(PROMOTE_SCRIPT, Long.class);
        Long promoted = redisTemplate.execute(
                script,
                List.of(waitingKey(courseId), activeKey(courseId)),
                String.valueOf(count),
                String.valueOf(expiredAt)
        );
        return promoted == null ? 0L : promoted;
    }

    @Override
    public void removeFromWaitingQueue(Long courseId, Long userId) {
        redisTemplate.opsForZSet().remove(waitingKey(courseId), userId.toString());
    }

    private String waitingKey(Long courseId) {
        return WAITING_KEY_PREFIX + courseId;
    }

    private String activeKey(Long courseId) {
        return ACTIVE_KEY_PREFIX + courseId;
    }

}
