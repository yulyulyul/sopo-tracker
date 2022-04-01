package team.sopo.infrastructure.carrierselector.kdexp

class KdResponse(
    val result: String,
    val items: List<Item>,
    val info: Info
){
    companion object{
        const val SUCCESS = "suc"
        const val FAIL = "fail"
    }
}
