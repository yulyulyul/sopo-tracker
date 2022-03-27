package team.sopo.domain.tracker

import team.sopo.common.parcel.Parcel
import team.sopo.common.parcel.Status

abstract class CarrierSelector {
    abstract fun support(carrierCode: String): Boolean
    abstract fun tracking(command: TrackerCommand.Tracking): Parcel
    protected abstract fun calculateStatus(criteria: String): Status
}