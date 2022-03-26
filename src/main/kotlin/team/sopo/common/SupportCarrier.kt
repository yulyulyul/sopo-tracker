package team.sopo.common

import team.sopo.common.parcel.Carrier

enum class SupportCarrier(val code: String, val cname: String, val tel: String) {
    CHUNILPS("kr.chunilps", "천일택배", "+8218776606"),
    CJ_LOGISTICS("kr.cjlogistics", "CJ대한통운", "+8215881255"),
    CU_POST("kr.cupost", "CU 편의점 택배", "+8215771287"),
    CVSNET("kr.cvsnet", "GS Postbox 택배", "+8215771287"),
    DAESIN("kr.daesin", "대신택배", "+82314620100"),
    EPOST("kr.epost", "우체국택배", "+8215881300"),
    HANJINS("kr.hanjin", "한진택배", "+8215880011"),
    HDEXP("kr.hdexp", "합동택배", "+8218993392"),
    KDEXP("kr.kdexp", "경동택배", "+8218995368"),
    LOGEN("kr.logen", "로젠택배", "+8215889988"),
    LOTTE("kr.lotte", "롯데택배", "+8215882121");

    companion object {
        fun toCarrier(code: String): Carrier {
            val supportCarrier = getSupportCarrier(code)
            return Carrier(
                supportCarrier.code,
                supportCarrier.cname,
                supportCarrier.tel
            )
        }
        fun getSupportCarrier(code: String): SupportCarrier {
            val carrier = values().findLast {
                it.code == code
            }
            carrier ?: throw IllegalStateException("정의되지 않은 배송사입니다.")
            return carrier
        }
    }
}