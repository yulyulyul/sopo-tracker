package team.sopo.infrastructure.carrierselector.cjlogistics

data class ParcelResult(
    val invcNo: String,
    val sendrNm: String,
    val qty: String,
    val itemNm: String,
    val rcvrNm: String,
    val rgmailNo: String,
    val oriTrspbillnum: String,
    val rtnTrspbillnum: String,
    val nsDlvNm: String
)