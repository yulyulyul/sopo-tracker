package team.sopo.common.parcel

import com.fasterxml.jackson.annotation.JsonProperty

data class Location(
    @JsonProperty("name") val name: String
)