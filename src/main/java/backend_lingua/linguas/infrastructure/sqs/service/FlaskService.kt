package backend_lingua.linguas.infrastructure.sqs.service

import backend_lingua.linguas.global.dto.HttpResponse
import backend_lingua.linguas.global.exception.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class FlaskService(
    @Value("\${kanji.api.base-url}")
    private val flaskUrl: String,
    private val restTemplate: RestTemplate
) {

    private val logger = LoggerFactory.getLogger(FlaskService::class.java)

    fun fetchByBookName(bookName: String): Map<String, Any> {
        return runCatching {
            val fullUrl = "$flaskUrl$bookName"
            logger.info("🔗 Flask API 호출 URL: $fullUrl")

            val responseType = object : ParameterizedTypeReference<Map<String, Any>>() {}
            val response = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET,
                null,
                responseType
            )
            logger.info("✅ Flask API 응답 수신 성공")
            response.body ?: emptyMap()
        }.getOrElse { exception ->
            logger.error("❌ Flask API 호출 실패: ${exception.message}")
            logger.error("상세 에러: ", exception)
            throw BusinessException(HttpResponse.FailureStatus.BAD_REQUEST)
        }
    }
}