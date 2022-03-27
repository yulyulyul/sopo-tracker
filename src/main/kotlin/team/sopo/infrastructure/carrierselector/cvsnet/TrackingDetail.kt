package team.sopo.infrastructure.carrierselector.cvsnet

data class TrackingDetail(
    val transKind: String,
    val level: Int,
    val transTime: String,
    val transWhere: String,
    val transCode: String
)
