package backend_lingua.linguas.kanji.api;

import backend_lingua.linguas.domain.kanji.enumerated.TaskStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("KanjiController 실제 API 통합 테스트")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)

public class FileMultithreadingUploadTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private record TaskStatusInfo(TaskStatus taskStatus, String status, String responseBody) {
    }

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private MockMultipartFile createRealPdfFile() {
        try {
            String filePath = "/Users/hwangjungseog/Downloads/단어.pdf";
            Path path = Paths.get(filePath);

            if (Files.exists(path)) {
                byte[] content = Files.readAllBytes(path);
                String fileName = path.getFileName().toString();

                System.out.println("📄 실제 PDF 파일 로드 성공: " + filePath);
                System.out.println("📋 파일명: " + fileName);
                System.out.println("📦 파일 크기: " + content.length + " bytes");

                return new MockMultipartFile(
                        "file",
                        fileName,
                        MediaType.APPLICATION_PDF_VALUE,
                        content
                );
            } else {
                throw new RuntimeException("PDF 파일을 찾을 수 없습니다: " + filePath);
            }

        } catch (IOException e) {
            throw new RuntimeException("PDF 파일 읽기 실패: " + e.getMessage(), e);
        }
    }

    private TaskStatusInfo getCurrentTaskStatus(Long taskId) throws Exception {
        MvcResult statusResult = mockMvc.perform(get("/api/files/{id}/status", taskId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists())
                .andReturn();

        String statusResponseBody = statusResult.getResponse().getContentAsString();
        JsonNode statusJson = objectMapper.readTree(statusResponseBody);
        String currentStatus = statusJson.get("status").asText();

        TaskStatus taskStatus = TaskStatus.fromStatus(currentStatus);

        return new TaskStatusInfo(taskStatus, currentStatus, statusResponseBody);
    }

    public void handlePendingResult(Long taskId) throws Exception {
        System.out.println("⏳ PENDING 상태 결과 조회...");

        MvcResult pendingResult = mockMvc.perform(get("/api/files/{id}/result", taskId))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andReturn();

        String responseBody = pendingResult.getResponse().getContentAsString();
        assertThat(pendingResult.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(responseBody).isEmpty();

        // TaskStatus의 메시지만 검증
        assertThat(TaskStatus.PENDING.getMessage(null)).contains("처리 대기");
    }

    public void handleProcessingResult(Long taskId) throws Exception {
        System.out.println("⚙️ PROCESSING 상태 결과 조회...");

        MvcResult processingResult = mockMvc.perform(get("/api/files/{id}/result", taskId))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andReturn();

        String responseBody = processingResult.getResponse().getContentAsString();

        System.out.println("3️⃣ PROCESSING 상태 결과:");
        System.out.println("📋 응답 상태: " + processingResult.getResponse().getStatus());
        System.out.println("📋 응답 본문: " + (responseBody.isEmpty() ? "없음 (예상됨)" : responseBody));

        // TaskStatus의 메시지만 검증
        assertThat(TaskStatus.PROCESSING.getMessage(null)).contains("처리 중");
    }

    public void handleDoneResult(Long taskId) throws Exception {
        System.out.println("✅ DONE 상태 결과 조회...");

        MvcResult doneResult = mockMvc.perform(get("/api/files/{id}/result", taskId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String doneResponseBody = doneResult.getResponse().getContentAsString();
        System.out.println("3️⃣ DONE 상태 결과:");
        System.out.println("📋 응답: " + doneResponseBody);

        if (!doneResponseBody.isEmpty()) {
            JsonNode doneJson = objectMapper.readTree(doneResponseBody);
            System.out.println("🎨 결과 JSON (Pretty):");
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doneJson));
            assertThat(doneJson).isNotNull();
        }
    }

    public void handleFailedResult(Long taskId) throws Exception {
        System.out.println("❌ FAILED 상태 결과 조회...");

        MvcResult failedResult = mockMvc.perform(get("/api/files/{id}/result", taskId))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andReturn();

        String responseBody = failedResult.getResponse().getContentAsString();

        System.out.println("3️⃣ FAILED 상태 결과:");
        System.out.println("📋 응답 상태: " + failedResult.getResponse().getStatus());
        System.out.println("📋 응답 본문: " + (responseBody.isEmpty() ? "없음 (예상됨)" : responseBody));
    }

    public void handleTaskStatusResult(Long taskId, TaskStatus taskStatus) throws Exception {
        switch (taskStatus) {
            case PENDING -> handlePendingResult(taskId);
            case PROCESSING -> handleProcessingResult(taskId);
            case DONE -> handleDoneResult(taskId);
            case FAILED -> handleFailedResult(taskId);
        }
    }


    @Test
    @DisplayName("동시 파일 업로드 테스트")
    void concurrent_Upload_Test() throws Exception {
        setUp();

        int threadCount = 2; // 실제 PDF 파일을 사용하므로 적당한 수로 조정
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Future<MvcResult>> futures = new ArrayList<>();

        System.out.println("🚀 실제 PDF 파일로 동시 업로드 테스트 시작 - " + threadCount + "개 요청");

        for (int i = 1; i <= threadCount; i++) {
            int requestIndex = i;
            Callable<MvcResult> task = () -> {
                try {
                    // 실제 PDF 파일 사용
                    MockMultipartFile realPdfFile = createRealPdfFile();

                    System.out.println("📤 요청 " + requestIndex + " - 파일 업로드 중...");

                    MvcResult result = mockMvc.perform(multipart("/api/files/upload").file(realPdfFile))
                            .andExpect(status().isAccepted())
                            .andExpect(jsonPath("$.id").exists())
                            .andExpect(jsonPath("$.message").exists())
                            .andReturn();

                    System.out.println("✅ 요청 " + requestIndex + " - 업로드 완료");
                    latch.countDown();
                    return result;

                } catch (Exception e) {
                    System.out.println("❌ 요청 " + requestIndex + " - 업로드 실패: " + e.getMessage());
                    latch.countDown();
                    throw new RuntimeException(e);
                }
            };
            futures.add(executorService.submit(task));
        }

        // 모든 요청 완료 대기 (최대 30초)
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executorService.shutdown();

        if (!completed) {
            System.out.println("⚠️ 일부 요청이 30초 내에 완료되지 않았습니다.");
        }

        Set<Long> taskIds = new HashSet<>();
        List<String> uploadResponses = new ArrayList<>();

        for (int i = 0; i < futures.size(); i++) {
            try {
                Future<MvcResult> future = futures.get(i);
                MvcResult result = future.get(5, TimeUnit.SECONDS); // 각 결과 최대 5초 대기

                String responseBody = result.getResponse().getContentAsString();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                Long taskId = jsonResponse.get("id").asLong();
                String message = jsonResponse.get("message").asText();

                taskIds.add(taskId);
                uploadResponses.add(String.format("Task ID: %d, Message: %s", taskId, message));

                System.out.println("✅ 요청 " + (i + 1) + " 결과 - Task ID: " + taskId);

            } catch (TimeoutException e) {
                System.out.println("⚠️ 요청 " + (i + 1) + " 결과 대기 시간 초과");
            } catch (Exception e) {
                System.out.println("❌ 요청 " + (i + 1) + " 결과 처리 실패: " + e.getMessage());
            }
        }

        // 검증
        System.out.println("\n📊 동시 업로드 테스트 결과:");
        System.out.println("🎯 성공한 업로드 수: " + taskIds.size() + "/" + threadCount);
        System.out.println("📋 생성된 Task IDs: " + taskIds);

        // 모든 작업 ID가 고유한지 확인
        assertThat(taskIds.size()).isEqualTo(threadCount);

        // 각 업로드 응답 출력
        System.out.println("\n📝 업로드 응답 상세:");
        uploadResponses.forEach(System.out::println);

        System.out.println("\n🎉 동시 업로드 테스트 완료!");

        // 추가 검증: 생성된 작업들의 초기 상태 확인
        System.out.println("\n🔍 생성된 작업들의 초기 상태 확인:");
        for (Long taskId : taskIds) {
            try {
                TaskStatusInfo statusInfo = getCurrentTaskStatus(taskId);
                System.out.println("Task ID " + taskId + " - 상태: " + statusInfo.status());
                assertThat(statusInfo.taskStatus()).isIn(TaskStatus.PENDING, TaskStatus.PROCESSING);
            } catch (Exception e) {
                System.out.println("⚠️ Task ID " + taskId + " 상태 확인 실패: " + e.getMessage());
            }
        }
    }
}
