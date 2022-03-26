package team.sopo.common.parcel

import com.fasterxml.jackson.annotation.JsonProperty

data class To(
    @JsonProperty("name") val name: String?,
    @JsonProperty("time") val time: String? = null
)