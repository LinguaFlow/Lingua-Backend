package backend_lingua.linguas.domain.example.api;

import backend_lingua.linguas.domain.example.service.ExampleService;
import backend_lingua.linguas.domain.example.dto.request.ExampleSentenceRequest;
import backend_lingua.linguas.domain.example.dto.response.ExampleSentencesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ExampleController {

    private final ExampleService example;

    // POST 방식 API 엔드포인트
    @PostMapping("/examples")
    public ResponseEntity<ExampleSentencesResponse> generateExamples(
            @RequestBody ExampleSentenceRequest request
    ) {
        ExampleSentencesResponse response = example.generateExamples(request);
        return ResponseEntity.ok(response);
    }
}
