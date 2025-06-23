package backend_lingua.linguas.kanji.service

import backend_lingua.linguas.util.exception.BusinessException
import backend_lingua.linguas.util.http.HttpResponse

import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.logging.Logger


@Service
@Slf4j
class FlaskService(
    @Value("\${kanji.api.base-url}")
    private val baseUrl: String,
    private val restTemplate: RestTemplate
) {

    fun fetchByBookName(bookName: String): Map<String, Any> {
        return runCatching {
            // 책 이름을 파라미터로 전달
            val url = "$baseUrl?book_name=$bookName"
//            log.info("🌐 Flask API 호출: {}", url)

            Logger.getAnonymousLogger().info(url)
            val responseType = object : ParameterizedTypeReference<Map<String, Any>>() {}
            val response = restTemplate.exchange(url, HttpMethod.GET, null, responseType)

            response.body ?: emptyMap()
        }.getOrElse { throwable ->

//            log.error("❌ Flask API 호출 실패: {}", throwable.message, throwable)
            throw BusinessException(HttpResponse.FailureStatus.BAD_REQUEST)
        }
    }
}