package team.sopo.common.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import team.sopo.common.exception.error.SopoError

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class ValidationException(message: String): SopoException(SopoError.VALIDATION, message)