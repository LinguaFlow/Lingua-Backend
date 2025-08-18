package backend_lingua.linguas.global.exception

import backend_lingua.linguas.global.dto.ErrorMessageResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice


@RestControllerAdvice
class ExceptionHandlerImpl() {

    @ExceptionHandler(BusinessException::class)
    fun handlerBusinessException(e: BusinessException, request: HttpServletRequest,exception: Exception): ResponseEntity<ErrorMessageResponse> {
        return ResponseEntity
            .status(e.httpStatus)
            .body(ErrorMessageResponse.of(
                e.message ,
                request ,
                exception))
    }
}