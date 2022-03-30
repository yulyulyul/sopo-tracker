package team.sopo.common.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import team.sopo.common.exception.error.SopoError

@ResponseStatus(value = HttpStatus.NOT_FOUND)
class ParcelNotFoundException(message: String= "해당하는 id에 부합하는 택배를 찾을 수 없습니다."): SopoException(SopoError.PARCEL_NOT_FOUND, message)