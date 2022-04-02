package team.sopo.common.parcel

import com.fasterxml.jackson.annotation.JsonProperty

class State(
    @JsonProperty("id") var id: String,
    @JsonProperty("text") var text: String
) {
    companion object {
        fun getInformationReceived(): State {
            return State("information_received", "상품준비중")
        }

        fun getAtPickUp(): State {
            return State("at_pickup", "상품인수")
        }

        fun getInTransit(): State {
            return State("in_transit", "상품이동중")
        }

        fun getOutForDelivery(): State {
            return State("out_for_delivery", "배송출발")
        }

        fun getDelivered(): State {
            return State("delivered", "배송완료")
        }
    }
}