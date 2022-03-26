package team.sopo.common.parcel

enum class DeliveryStatus(val id: String, val description: String) {
    NOT_REGISTERED("not_registered", "등록되지 않았음"),
    CHANGED("changed", "상태가 변경되었음"),
    UNCHANGED("unchanged", "상태가 변경되지 않았음"),
    ORPHANED("orphaned", "택배가 오랫동안 변경이 없음"),
    DELIVERED("delivered", "배송완료"), // 배송완료
    OUT_FOR_DELIVERY("out_for_delivery", "배송출발"), // 배송출발
    IN_TRANSIT("in_transit", "상품이동중"), // 상품이동중
    AT_PICKUP("at_pickup", "상품인수"), // 상품인수
    INFORMATION_RECEIVED("information_received", "상품준비중") // 상품준비중
}