package backend_lingua.linguas.util.dto.response

import jakarta.servlet.http.HttpServletRequest

data class ErrorMessageResponse(
    val httpMethod: String,
    val path: String,
    val message: String,
    val exception: Exception,
) {
    companion object {
        fun of(message: String, httpRequest: HttpServletRequest, exception: Exception): ErrorMessageResponse {
            return ErrorMessageResponse(
                httpMethod = httpRequest.method.toString(),
                path = httpRequest.method,
                message = message,
                exception = exception
            )
        }
    }
}