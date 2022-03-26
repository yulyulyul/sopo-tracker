package team.sopo.domain.tracker

import org.springframework.stereotype.Service
import team.sopo.common.exception.InsufficientConditionException
import team.sopo.common.parcel.Parcel

@Service
class TrackerServiceImpl(private val carrierSelectors: List<CarrierSelector>): TrackerService {

    override fun tracking(command: TrackerCommand.Tracking): Parcel {
        val carrierSelector = routingCarrierSelector(command)
        return carrierSelector.tracking(command)
    }

    private fun routingCarrierSelector(command: TrackerCommand.Tracking): CarrierSelector {
        return carrierSelectors.stream()
            .filter { carrierSelectors -> carrierSelectors.support(command.carrierCode) }
            .findFirst()
            .orElseThrow { InsufficientConditionException("지원하는 배송사가 아닙니다.") }
    }
}