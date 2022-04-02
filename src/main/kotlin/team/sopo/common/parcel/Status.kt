package team.sopo.common.parcel

import com.fasterxml.jackson.annotation.JsonProperty

data class Status(
    @JsonProperty("id") val id: String,
    @JsonProperty("text") val text: String
){
    companion object {
        fun getInformationReceived(): Status {
            return Status("information_received", "상품준비중")
        }

        fun getAtPickUp(): Status{
            return Status("at_pickup","상품인수")
        }

        fun getInTransit(): Status{
            return Status("in_transit","상품이동중")
        }

        fun getOutForDelivery(): Status{
            return Status("out_for_delivery","배송출발")
        }

        fun getDelivered(): Status{
            return Status("delivered","배송완료")
        }
    }
}