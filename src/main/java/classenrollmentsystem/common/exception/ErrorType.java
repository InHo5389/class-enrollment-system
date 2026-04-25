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
    DUPLICATE_CREATOR_PROFILE(400, "이미 크리에이터로 등록된 사용자입니다."),

    COURSE_NOT_FOUND(404, "존재하지 않는 강의입니다."),
    COURSE_NOT_OWNER(403, "강의를 수정할 권한이 없습니다."),
    INVALID_COURSE_CAPACITY(400, "정원은 1명 이상이어야 합니다."),
    INVALID_COURSE_PERIOD(400, "종료일이 시작일보다 이전일 수 없습니다."),
    INVALID_COURSE_STATUS_TRANSITION(400, "허용되지 않는 상태 변경입니다.");

    private final int status;
    private final String message;

}
