package team.sopo.common.parcel

import com.fasterxml.jackson.annotation.JsonProperty

data class From(
    @JsonProperty("name") var name: String?,
    @JsonProperty("time") var time: String? = null,
    @JsonProperty("tel") var tel: String? = null
)