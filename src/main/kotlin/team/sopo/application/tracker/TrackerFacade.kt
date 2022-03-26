package team.sopo.application.tracker

import org.springframework.stereotype.Service
import team.sopo.common.parcel.Parcel
import team.sopo.domain.tracker.TrackerCommand
import team.sopo.domain.tracker.TrackerService

@Service
class TrackerFacade(private val trackerService: TrackerService) {
    fun tracking(command: TrackerCommand.Tracking): Parcel {
        return trackerService.tracking(command)
    }
}