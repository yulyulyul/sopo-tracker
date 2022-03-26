package team.sopo.common.parcel

import com.fasterxml.jackson.annotation.JsonProperty

data class Progresses(
    @JsonProperty("time") var time: String? = null,
    @JsonProperty("location") var location: Location? = null,
    @JsonProperty("status") var status: Status,
    @JsonProperty("description") var description: String? = null
)