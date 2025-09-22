package backend_lingua.linguas.global.dto

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
        KANJI_TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "한자 작업을 찾을 수 없습니다."),
        USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
        SQS_MESSAGE_PARSE_ERROR(HttpStatus.BAD_REQUEST, "SQS 메시지 파싱에 실패했습니다."),
        SQS_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SQS 메시지 처리 중 오류가 발생했습니다."),
        SQS_KANJI_DATA_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "한자 데이터 처리에 실패했습니다."),
        SQS_FLASK_SERVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "Flask 서비스 연결에 실패했습니다."),
        OAUTH_USER_INFO_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "소셜 로그인 사용자 정보 조회에 실패했습니다."),
    }
}