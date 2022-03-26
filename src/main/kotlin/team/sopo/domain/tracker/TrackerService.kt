package team.sopo.domain.tracker

import team.sopo.common.parcel.Parcel

interface TrackerService {
    fun tracking(command: TrackerCommand.Tracking): Parcel
}