package team.sopo.common.exception.error

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.Gson
import team.sopo.common.exception.SopoException

class Error(
    @JsonProperty("code")
    val code: Int,
    @JsonProperty("type")
    val type: ErrorType,
    @JsonProperty("message")
    val message: String,
    @JsonProperty("path")
    var path: String
){
    constructor(sopoException: SopoException, path: String):this(
        code = sopoException.sopoError.code,
        type = sopoException.sopoError.type,
        message = sopoException.getErrorMessage(),
        path = path
    )

    constructor(sopoError: SopoError, message: String, path: String): this(
        code = sopoError.code,
        type = sopoError.type,
        message = message,
        path = path
    )

    override fun toString(): String {
        return Gson().toJson(this)
    }
}