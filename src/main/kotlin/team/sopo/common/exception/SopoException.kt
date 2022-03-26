package team.sopo.common.exception

import org.springframework.http.HttpStatus
import team.sopo.common.exception.error.SopoError

abstract class SopoException(val sopoError: SopoError, var errMsg: String?) : RuntimeException(sopoError.message) {

    fun getHttpStatus(): HttpStatus{
        return sopoError.status
    }

    fun getErrorMessage(): String{
        return if(sopoError.message.isNullOrBlank()){
            errMsg ?: throw NullPointerException("${sopoError.name}의 에러메시지는 정의되지 않았습니다.")
        } else{
            sopoError.message
        }
    }
}