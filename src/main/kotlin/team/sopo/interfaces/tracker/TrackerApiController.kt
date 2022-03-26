package team.sopo.interfaces.tracker

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import team.sopo.application.tracker.TrackerFacade
import team.sopo.common.parcel.Parcel
import team.sopo.domain.tracker.TrackerCommand
import javax.validation.constraints.NotNull

@RestController
@RequestMapping("/api/v1/sopo-tracker/tracking")
class TrackerApiController(
    private val trackerFacade: TrackerFacade
) {
    @GetMapping("{carrier}/tracks/{waybillNum}")
    fun parcelTracking(
        @PathVariable("carrier", required = true)
        @NotNull(message = "배송사를 확인해주세요.")
        carrier: String? = null,
        @PathVariable("waybillNum", required = true)
        @NotNull(message = "송장번호를 확인해주세요.")
        waybillNum: String? = null
    ): ResponseEntity<Parcel> {
        return ResponseEntity.ok(trackerFacade.tracking(TrackerCommand.Tracking(carrier!!, waybillNum!!)))
    }
}