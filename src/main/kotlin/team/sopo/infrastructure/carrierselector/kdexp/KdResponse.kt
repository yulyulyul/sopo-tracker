package team.sopo.infrastructure.carrierselector.kdexp

import team.sopo.common.SupportCarrier
import team.sopo.common.extension.removeSpecialCharacter
import team.sopo.common.extension.sortProgress
import team.sopo.common.parcel.*
import kotlin.streams.toList

data class KdResponse(
    val result: String,
    val items: List<Item>,
    val info: Info
){
    fun toParcel(): Parcel {
        var parcel = Parcel(carrier = SupportCarrier.toCarrier(SupportCarrier.KDEXP.code))

        parcel.from = From(info.send_name, info.pd_dt, null)
        parcel.to = To(info.re_name, info.rec_dt)
        parcel.item = info.prod

        val progresses = items.stream().map { item ->
            Progresses(
                time = item.reg_date,
                location = Location(item.location),
                status = calculateStatus(item.stat),
                description = item.stat
            )
        }.toList()
        parcel.progresses.addAll(progresses)
        parcel = sorting(parcel)
        parcel.state = calculateState(parcel)

        return parcel
    }

    private fun calculateStatus(criteria: String): Status {
        return when (criteria) {
            "접수완료" -> Status.getInformationReceived()
            "영업소집하" -> Status.getAtPickUp()
            "배송완료" -> Status.getDelivered()
            else -> Status.getInTransit()
        }
    }

    private fun sorting(parcel: Parcel): Parcel {
        return parcel.apply {
            parcel.removeSpecialCharacter()
            parcel.sortProgress()
        }
    }

    private fun calculateState(parcel: Parcel): State {
        val status = parcel.progresses.lastOrNull()?.status ?: Status.getInformationReceived()
        return State(status.id, status.text)
    }
}
