package team.sopo.common.exception

import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import team.sopo.common.exception.error.Error


@Component
class GlobalErrorAttributes : DefaultErrorAttributes() {

    companion object {
        private const val CODE = "code"
        private const val TYPE = "type"
        private const val ERROR = "error"
        private const val PATH = "path"
    }

    override fun getErrorAttributes(request: ServerRequest, options: ErrorAttributeOptions): MutableMap<String, Any> {

        val errorAttributes = super.getErrorAttributes(request, options)
        val exception = getError(request)

        if (exception is SopoException) {
            val error = Error(exception, request.path())
            errorAttributes[CODE] = error.code
            errorAttributes[TYPE] = error.type
            errorAttributes[ERROR] = error.message
            errorAttributes[PATH] = error.path
        }

        return errorAttributes
    }
}