package team.sopo.infrastructure.carrierselector.cvsnet

data class GsResponse(
    val sender: Sender,
    val receiver: Receiver,
    val latestTrackingDetail: TrackingDetail,
    val trackingDetails: List<TrackingDetail>,
    val carrierType: String,
    val carrierName: String,
    val code: Int,
    val serviceType: String,
    val invoiceNo: String,
    val goodsName: String,
    val serviceName: String
)