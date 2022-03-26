package team.sopo.common.exception

import team.sopo.common.exception.error.SopoError

class InsufficientConditionException(message: String): SopoException(SopoError.INSUFFICIENT_CONDITION, message)