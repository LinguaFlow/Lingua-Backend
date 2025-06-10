package backend_lingua.linguas.util.http

import lombok.Getter
import lombok.RequiredArgsConstructor
import org.springframework.http.HttpStatus

class HttpResponse {

    @Getter
    @RequiredArgsConstructor
    enum class SuccessStatus(
        val status: HttpStatus,
        val message: String
    ) {
        OK(HttpStatus.OK, "업로드 성공"),
    }


    @Getter
    @RequiredArgsConstructor
    enum class FailureStatus(
        val status: HttpStatus,
        val message: String
    ) {
        BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 입력값 입니다."),
    }
}