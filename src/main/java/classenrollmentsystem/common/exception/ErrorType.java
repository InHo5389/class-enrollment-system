package classenrollmentsystem.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorType {

    DUPLICATE_EMAIL(400, "이미 가입된 이메일입니다."),
    USER_NOT_FOUND(404, "존재하지 않는 사용자입니다."),
    INVALID_PASSWORD(400, "비밀번호가 일치하지 않습니다."),
    INVALID_EMAIL(400, "존재하지 않는 이메일입니다."),
    MISSING_USER_ID_HEADER(401, "사용자 인증 토큰(X-User-Id 헤더)이 필요합니다."),

    NOT_CREATOR(403, "크리에이터 권한이 필요합니다."),
    DUPLICATE_CREATOR_PROFILE(400, "이미 크리에이터로 등록된 사용자입니다.");

    private final int status;
    private final String message;

}
