package backend_lingua.linguas.kanji.service

import backend_lingua.linguas.util.exception.BusinessException
import backend_lingua.linguas.util.http.HttpResponse
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

    fun fetchAll(): Map<String, Any> {

        return runCatching {
            val responseType = object : ParameterizedTypeReference<Map<String, Any>>() {}
            val response = restTemplate.exchange(
                flaskUrl,
                HttpMethod.GET,
                null,
                responseType
            )
            response.body ?: emptyMap()
        }.getOrElse {
            throw BusinessException(HttpResponse.FailureStatus.BAD_REQUEST)
        }
    }
}