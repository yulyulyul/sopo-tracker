package team.sopo.infrastructure.carrierselector.cjlogistics

data class ParcelDetailResult(
    val nsDlvNm: String,
    val crgNm: String?,
    val crgSt: String,
    val dTime: String,
    val empImgNm: String,
    val regBranId: String,
    val regBranNm: String,
    val scanNm: String
)
