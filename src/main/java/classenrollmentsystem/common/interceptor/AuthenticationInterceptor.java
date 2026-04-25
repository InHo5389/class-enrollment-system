package classenrollmentsystem.common.interceptor;

import classenrollmentsystem.common.exception.CustomGlobalException;
import classenrollmentsystem.common.exception.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    private static final String USER_ID_HEADER = "X-USER-ID";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String userIdHeader = request.getHeader(USER_ID_HEADER);

        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            log.warn("인증 실패: X-User-Id 헤더 누락 - 경로: {}", request.getRequestURI());
            throw new CustomGlobalException(ErrorType.MISSING_USER_ID_HEADER);
        }

        try {
            Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            log.warn("인증 실패: 유효하지 않은 X-User-Id 값 - 값: {}", userIdHeader);
            throw new CustomGlobalException(ErrorType.MISSING_USER_ID_HEADER);
        }

        return true;
    }
}
