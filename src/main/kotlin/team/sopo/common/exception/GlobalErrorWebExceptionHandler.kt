package team.sopo.common.exception

import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.RequestPredicates.all
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono

@Component
@Order(-2)
class GlobalErrorWebExceptionHandler(
    globalErrorAttributes: GlobalErrorAttributes,
    applicationContext: ApplicationContext,
    codecConfigurer: ServerCodecConfigurer
) : AbstractErrorWebExceptionHandler(globalErrorAttributes, WebProperties.Resources(), applicationContext) {

    companion object {
        private const val STATUS = "status"
    }

    init {
        super.setMessageWriters(codecConfigurer.writers)
        super.setMessageReaders(codecConfigurer.readers)
    }

    override fun getRoutingFunction(errorAttributes: ErrorAttributes?): RouterFunction<ServerResponse> {
        return router { (all()) { renderErrorResponse(it) } }
    }

    private fun renderErrorResponse(request: ServerRequest): Mono<ServerResponse> {
        val errorPropertiesMap = getErrorAttributes(
            request,
            ErrorAttributeOptions.defaults()
        )

        val status = (errorPropertiesMap[STATUS] as Int)
            .let { HttpStatus.valueOf(it) }

        return ServerResponse.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(errorPropertiesMap))
    }

}