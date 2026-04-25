package classenrollmentsystem.common.interceptor;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import classenrollmentsystem.common.interceptor.annotation.PublicApi;
import classenrollmentsystem.common.interceptor.annotation.RequireCreator;
import classenrollmentsystem.user.service.CreatorProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationInterceptor implements HandlerInterceptor {

    private static final String USER_ID_HEADER = "X-USER-ID";

    private final CreatorProfileRepository creatorProfileRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (handlerMethod.hasMethodAnnotation(PublicApi.class)) {
            return true;
        }

        Long userId = extractUserId(request);

        if (handlerMethod.hasMethodAnnotation(RequireCreator.class)) {
            validateCreator(userId);
        }

        return true;
    }

    private Long extractUserId(HttpServletRequest request) {
        String userIdHeader = request.getHeader(USER_ID_HEADER);

        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            log.warn("인증 실패: X-User-Id 헤더 누락 - 경로: {}", request.getRequestURI());
            throw new CustomGlobalException(ErrorType.MISSING_USER_ID_HEADER);
        }

        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            log.warn("인증 실패: 유효하지 않은 X-User-Id 값 - 값: {}", userIdHeader);
            throw new CustomGlobalException(ErrorType.MISSING_USER_ID_HEADER);
        }
    }

    private void validateCreator(Long userId) {
        if (!creatorProfileRepository.existsByUserId(userId)) {
            log.warn("권한 없음: 크리에이터가 아닌 사용자 접근 - 사용자 ID: {}", userId);
            throw new CustomGlobalException(ErrorType.NOT_CREATOR);
        }
    }
}
