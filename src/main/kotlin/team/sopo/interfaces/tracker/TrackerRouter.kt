package team.sopo.interfaces.tracker

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse

@Configuration(proxyBeanMethods = false)
class TrackerRouter {

    @Bean
    fun route(trackerHandler: TrackerHandler): RouterFunction<ServerResponse> {
        return RouterFunctions
            .route()
            .GET("/api/v1/sopo-tracker/tracking", accept(MediaType.APPLICATION_JSON), trackerHandler::tracking)
            .build()
    }
}