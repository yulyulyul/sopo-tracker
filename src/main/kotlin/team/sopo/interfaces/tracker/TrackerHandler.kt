package team.sopo.interfaces.tracker

import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import team.sopo.application.tracker.TrackerFacade
import team.sopo.common.exception.ValidationException
import team.sopo.domain.tracker.TrackerCommand

@Component
class TrackerHandler(private val trackerFacade: TrackerFacade) {

    fun tracking(request: ServerRequest): Mono<ServerResponse>{
        val waybillNum = request.queryParam("waybillNum").orElseThrow { ValidationException("송장번호를 확인해주세요.") }
        val carrier = request.queryParam("carrier").orElseThrow { ValidationException("배송사를 확인해주세요.") }

        return ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(trackerFacade.tracking(TrackerCommand.Tracking(carrier, waybillNum)))
    }

}