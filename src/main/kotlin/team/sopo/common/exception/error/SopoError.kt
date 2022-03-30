package team.sopo.common.exception.error

import org.springframework.http.HttpStatus

enum class SopoError(
    val status: HttpStatus,
    val type: ErrorType,
    val code: Int,
    val message: String?) {

    // Common
    AUTHORIZE_FAIL(HttpStatus.FORBIDDEN, ErrorType.AUTHORIZE, 101, "허가되지 않은 접근 입니다."),
    AUTHENTICATION_FAIL(HttpStatus.UNAUTHORIZED, ErrorType.AUTHENTICATION, 102, "인증에 실패한 유저입니다."),
    VALIDATION(HttpStatus.BAD_REQUEST, ErrorType.VALIDATION, 103, null),

    PARCEL_NOT_FOUND(HttpStatus.NOT_FOUND, ErrorType.NO_RESOURCE, 703, null),
    INSUFFICIENT_CONDITION(HttpStatus.CONFLICT, ErrorType.CONFLICT, 105, null),
    UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ErrorType.UNKNOWN_ERROR, 199,"현재 서비스를 이용할 수 없습니다. 다음에 다시 시도해주세요."),
    SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ErrorType.SYSTEM, 999, null),
}