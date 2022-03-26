package team.sopo.domain.tracker

import team.sopo.common.parcel.Parcel

interface CarrierSelector {
    fun support(carrierCode: String): Boolean
    fun tracking(command: TrackerCommand.Tracking): Parcel
//    fun calculateState(criteria: String): State
}