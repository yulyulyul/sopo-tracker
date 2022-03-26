package team.sopo.infrastructure.carrierselector.cjlogistics

import team.sopo.common.SupportCarrier
import team.sopo.common.parcel.*
import kotlin.streams.toList

data class CjResponse(
    val parcelResultMap: ParcelResultMap,
    val parcelDetailResultMap: ParcelDetailResultMap
) {
    fun toParcel(carrierCode: String): Parcel {
        val parcel = Parcel(carrier = SupportCarrier.toCarrier(carrierCode))
        val parcelResult = parcelResultMap.resultList.firstOrNull()
            ?: throw IllegalStateException("Cj Response에서 ResultList를 추출할 수 없습니다.")
        val statePair = calculateState(parcelResult.nsDlvNm)

        parcel.from = From(name = parcelResult.sendrNm, time = null, tel = null)
        parcel.to = To(name = parcelResult.rcvrNm, null)
        parcel.item = parcelResult.itemNm
        parcel.state = State(statePair.first, statePair.second)

        val progresses = parcelDetailResultMap.resultList.stream().map { detail ->
            val statusPair = calculateState(detail.crgSt)
            Progresses(
                time = detail.dTime,
                location = Location(detail.regBranNm),
                status = Status(statusPair.first, statusPair.second),
                description = detail.crgNm
            )
        }.toList()
        parcel.progresses.addAll(progresses)

        return parcel
    }

    private fun calculateState(criteria: String?): Pair<String, String> {
        return when (criteria) {
            null -> Pair(State.getInformationReceived().id, State.getInformationReceived().text)
            "11" -> Pair(State.getAtPickUp().id, State.getAtPickUp().text)
            "41" -> Pair(State.getInTransit().id, State.getInTransit().text)
            "42" -> Pair(State.getInTransit().id, State.getInTransit().text)
            "44" -> Pair(State.getInTransit().id, State.getInTransit().text)
            "82" -> Pair(State.getOutForDelivery().id, State.getOutForDelivery().text)
            "91" -> Pair(State.getDelivered().id, State.getDelivered().text)
            else -> throw IllegalStateException("존재하지 않는 배송상태 입니다.(CJ)")
        }
    }

}
