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
    INVALID_COURSE_STATUS_TRANSITION(400, "허용되지 않는 상태 변경입니다."),

    ENROLLMENT_NOT_FOUND(404, "존재하지 않는 수강 신청입니다."),
    ENROLLMENT_NOT_OWNER(403, "수강 신청을 수정할 권한이 없습니다."),
    ENROLLMENT_OWN_COURSE(400, "본인이 개설한 강의에는 수강 신청할 수 없습니다."),
    ENROLLMENT_ALREADY_EXISTS(400, "이미 신청한 강의입니다."),
    ENROLLMENT_COURSE_NOT_OPEN(400, "모집 중인 강의에만 수강 신청할 수 있습니다."),
    ENROLLMENT_CAPACITY_EXCEEDED(400, "수강 정원이 초과되었습니다."),
    ENROLLMENT_INVALID_STATUS_TRANSITION(400, "허용되지 않는 수강 신청 상태 변경입니다."),
    ENROLLMENT_CANCEL_NOT_ALLOWED(400, "취소할 수 없는 상태입니다."),
    ENROLLMENT_CANCEL_DEADLINE_EXCEEDED(400, "취소 가능 기간이 지났습니다. 결제 후 7일 이내에만 취소할 수 있습니다.");

    private final int status;
    private final String message;

}
