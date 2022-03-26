package team.sopo.common.extension

import team.sopo.common.parcel.*

fun Parcel.removeSpecialCharacter(): Parcel {
    return removeSpecialCharacterInTrackInfo(this)
}

fun Parcel.sortProgress() {
    return this.progresses.sortWith(compareBy {
        when (it?.status?.id?.uppercase()) {
            DeliveryStatus.NOT_REGISTERED.name -> 0
            DeliveryStatus.INFORMATION_RECEIVED.name -> 1
            DeliveryStatus.AT_PICKUP.name -> 2
            DeliveryStatus.IN_TRANSIT.name -> 3
            DeliveryStatus.OUT_FOR_DELIVERY.name -> 4
            DeliveryStatus.DELIVERED.name -> 5
            else -> 999
        }
    })
}

private fun removeSpecialCharacterInTrackInfo(parcel: Parcel): Parcel {
    val declaredFields = Parcel::class.java.declaredFields
    for (field in declaredFields) {
        field.isAccessible = true
        val trackInfoAny = field.get(parcel)
        if (trackInfoAny != null) {
            when (field.genericType) {
                From::class.java -> {
                    removeSpecialCharacterByReflection(trackInfoAny, From::class.java)
                }
                To::class.java -> {
                    removeSpecialCharacterByReflection(trackInfoAny, To::class.java)
                }
                Parcel::item::class.java -> {
                    removeSpecialCharacterByReflection(trackInfoAny, Parcel::item::class.java)
                }
                State::class.java -> {
                    removeSpecialCharacterByReflection(trackInfoAny, State::class.java)
                }
                Carrier::class.java -> {
                    removeSpecialCharacterByReflection(trackInfoAny, Carrier::class.java)
                }
            }
            if (java.util.Collection::class.java.isAssignableFrom(trackInfoAny.javaClass)) {
                val listOfProgress: List<Progresses> = trackInfoAny as List<Progresses>
                listOfProgress.forEach { progresses ->
                    Progresses::class.java.declaredFields.forEach {
                        it.isAccessible = true
                        it.get(progresses)?.apply{
                            when (this::class.java) {
                                String::class.java -> {
                                    it.set(progresses, removeSpecialCharacter(this as String?))
                                }
                                Location::class.java -> {
                                    removeSpecialCharacterByReflection(this, Location::class.java)
                                }
                                Status::class.java -> {
                                    removeSpecialCharacterByReflection(this, Status::class.java)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    return parcel
}

private fun <T : Any> removeSpecialCharacterByReflection(obj: Any, classType: Class<T>) {
    if (classType.isInstance(obj) && obj::class.java == classType) {
        val objInstance = classType.cast(obj)
        classType.declaredFields.forEach {
            it.isAccessible = true
            val data = it.get(objInstance)
            if(data is String){
                it.set(objInstance, removeSpecialCharacter(data))
            }
        }
    }
}

private fun removeSpecialCharacter(str: String?): String? {
    if (str == null)
        return null
    if (str.isEmpty())
        return str
    return str.replace("\n", "").replace("\t", "").trim()
}