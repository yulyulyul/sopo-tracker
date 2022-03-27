package team.sopo.common.util

import team.sopo.common.extension.removeSpecialCharacter
import team.sopo.common.extension.sortProgress
import team.sopo.common.parcel.Parcel
import team.sopo.common.parcel.State
import team.sopo.common.parcel.Status

class ParcelUtil {
    companion object {
        fun sorting(parcel: Parcel): Parcel{
            return parcel.apply {
                parcel.removeSpecialCharacter()
                parcel.sortProgress()
            }
        }

        fun determineState(parcel: Parcel): State {
            val status = parcel.progresses.lastOrNull()?.status ?: Status.getInformationReceived()
            return State(status.id, status.text)
        }
    }
}