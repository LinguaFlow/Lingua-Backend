package backend_lingua.linguas.example.service;

import backend_lingua.linguas.example.dto.request.ExampleSentenceRequest;
import backend_lingua.linguas.example.dto.response.ExampleSentencesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;


@Slf4j
@Service
@RequiredArgsConstructor
public class ExampleService {

    private final RestTemplate restTemplate;

    @Value("${kanji.api.url}/api/examples")
    private String examplesApiUrl;

    public ExampleSentencesResponse generateExamples(ExampleSentenceRequest request) {
        try {
            HttpEntity<ExampleSentenceRequest> httpEntity = new HttpEntity<>(request, defaultHeaders());
            ResponseEntity<ExampleSentencesResponse> response = restTemplate.exchange(
                    examplesApiUrl,
                    HttpMethod.POST,
                    httpEntity,
                    ExampleSentencesResponse.class
            );
            log.info("response: {}", response.getBody());
            return handleResponse(response);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "예문 생성 중 오류가 발생했습니다.",
                    ex
            );
        }
    }



    /**
     * 공통 HTTP 헤더 세팅 (필요시 수정)
     */
    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * API 응답을 검증하고, 바디를 반환하거나 예외를 던집니다.
     */
    private ExampleSentencesResponse handleResponse(ResponseEntity<ExampleSentencesResponse> response) {
        HttpStatusCode status = response.getStatusCode();

        if (status.is2xxSuccessful()) {
            ExampleSentencesResponse body = response.getBody();
            if (body != null) {
                return body;
            }
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "API 응답 바디가 비어 있습니다."
            );
        }
        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "API 호출 중 오류가 발생했습니다: " + status
        );
    }
}