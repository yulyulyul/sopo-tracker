package team.sopo.domain.tracker

class TrackerCommand {
    data class Tracking(
        val carrierCode: String,
        val waybillNum: String
    )
}