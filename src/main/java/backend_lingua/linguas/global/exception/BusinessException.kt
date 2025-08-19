package backend_lingua.linguas.global.exception

import backend_lingua.linguas.global.dto.HttpResponse
import org.springframework.http.HttpStatus

data class BusinessException(
    val httpStatus: HttpStatus,
    override val message: String
) : RuntimeException(message) {

    constructor(failureStatus: HttpResponse.FailureStatus) : this(
        httpStatus = failureStatus.status,
        message = failureStatus.message
    )
}