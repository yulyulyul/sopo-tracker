package team.sopo.common.parcel

import com.fasterxml.jackson.annotation.JsonProperty

data class Parcel(
    @JsonProperty("from") var from: From? = null,
    @JsonProperty("to") var to: To? = null,
    @JsonProperty("state") var state: State? = null,
    @JsonProperty("item") var item: String? = null,
    @JsonProperty("progresses") var progresses: MutableList<Progresses?> = mutableListOf(),
    @JsonProperty("carrier") val carrier: Carrier
)