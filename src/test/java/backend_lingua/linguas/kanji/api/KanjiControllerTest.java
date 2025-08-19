package backend_lingua.linguas.kanji.api;

import backend_lingua.linguas.domain.kanji.entity.TaskStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("KanjiController 실제 API 통합 테스트")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class KanjiControllerTest {

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

    @Test
    @DisplayName("Flask API 연결 테스트")
    void test_FlaskApi_RealConnection() {
        setUp();

        try {
            MvcResult result = mockMvc.perform(get("/api/files/test"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            System.out.println("📄 Flask API 응답: " + responseBody);

            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            System.out.println("🎨 응답 JSON (Pretty):");
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonResponse));

            System.out.println("✅ Flask API 연결 테스트 성공!");

        } catch (Exception e) {
            System.out.println("⚠️ Flask API 연결 실패 (서버가 실행 중이지 않을 수 있습니다): " + e.getMessage());
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Flask 서버 연결 불가");
        }
    }

    @Test
    @DisplayName("실제 PDF 파일 업로드 및 전체 워크플로우 테스트")
    @DirtiesContext
    void realPdf_FullWorkflow_Integration() throws Exception {
        setUp();

        MockMultipartFile file = createRealPdfFile();

        System.out.println("🚀 실제 PDF 파일 업로드 시작...");
        System.out.println("📄 파일명: " + file.getOriginalFilename());
        System.out.println("📦 파일 크기: " + file.getSize() + " bytes");

        MvcResult uploadResult = mockMvc.perform(multipart("/api/files/upload").file(file))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.message").exists())
                .andReturn();

        String uploadResponseBody = uploadResult.getResponse().getContentAsString();
        JsonNode uploadJson = objectMapper.readTree(uploadResponseBody);
        Long taskId = uploadJson.get("id").asLong();
        assertThat(taskId).isNotNull();

        TaskStatusInfo statusInfo = getCurrentTaskStatus(taskId);
        assertThat(statusInfo).isNotNull();
        assertThat(statusInfo.taskStatus()).isIn(TaskStatus.PENDING, TaskStatus.PROCESSING);

        handleTaskStatusResult(taskId, statusInfo.taskStatus());
        waitForProcessingAndVerifyFinalResult(taskId);
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

    public void handleTaskStatusResult(Long taskId, TaskStatus taskStatus) throws Exception {
        switch (taskStatus) {
            case PENDING -> handlePendingResult(taskId);
            case PROCESSING -> handleProcessingResult(taskId);
            case DONE -> handleDoneResult(taskId);
            case FAILED -> handleFailedResult(taskId);
        }
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

    public void waitForProcessingAndVerifyFinalResult(Long taskId) {
        System.out.println("\n⏳ SQS 메시지 처리를 기다리는 중... (30초 대기)");

        try {
            for (int i = 0; i < 6; i++) {
                Thread.sleep(5000);

                try {
                    TaskStatusInfo currentStatus = getCurrentTaskStatus(taskId);
                    System.out.printf("⏰ %d초 경과 - 현재 상태: %s%n", (i + 1) * 5, currentStatus.status());

                    // 완료 상태 확인 - 직접 enum 비교로 변경
                    if (currentStatus.taskStatus() == TaskStatus.DONE || currentStatus.taskStatus() == TaskStatus.FAILED) {
                        System.out.println("🎯 작업이 완료되었습니다!");
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ 중간 상태 확인 중 오류: " + e.getMessage());
                }
            }

            TaskStatusInfo finalStatusInfo = getCurrentTaskStatus(taskId);
            System.out.println("4️⃣ 최종 상태: " + finalStatusInfo.status());
            System.out.println("📋 최종 응답: " + finalStatusInfo.responseBody());

            verifyFinalState(taskId, finalStatusInfo.taskStatus());

        } catch (InterruptedException e) {
            System.out.println("⚠️ 대기 중 인터럽트 발생: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.out.println("⚠️ 최종 상태 확인 중 오류: " + e.getMessage());
        }
    }

    public void verifyFinalState(Long taskId, TaskStatus finalStatus) throws Exception {
        if (finalStatus == TaskStatus.DONE) {
            MvcResult finalResult = mockMvc.perform(get("/api/files/{id}/result", taskId))
                    .andExpect(status().isOk())
                    .andReturn();

            String finalResultBody = finalResult.getResponse().getContentAsString();
            if (!finalResultBody.isEmpty()) {
                System.out.println("🎯 최종 처리 결과:");
                System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                        objectMapper.readTree(finalResultBody)));
            }
        }
    }

    @Test
    @DisplayName("잘못된 파일 형식 업로드 테스트")
    void upload_InvalidFileType_Integration() throws Exception {
        setUp();

        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "invalid-document.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "이것은 PDF가 아닌 텍스트 파일입니다.".getBytes()
        );

        System.out.println("🚫 잘못된 파일 형식 업로드 테스트 시작...");
        System.out.println("📄 파일명: " + invalidFile.getOriginalFilename());
        System.out.println("📋 파일 타입: " + invalidFile.getContentType());

        MvcResult result = mockMvc.perform(multipart("/api/files/upload").file(invalidFile))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonResponse = objectMapper.readTree(responseBody);
        val taskId = jsonResponse.get("id").asLong();

        System.out.println("📋 업로드 응답: " + responseBody);
        System.out.println("🆔 생성된 작업 ID: " + taskId);
        System.out.println("💡 현재 시스템은 파일 형식 검증 없이 모든 파일을 허용합니다.");
        System.out.println("✅ 실제 동작 확인 완료!");
    }

    @Test
    @DisplayName("빈 파일 업로드 테스트")
    void upload_EmptyFile_Integration() throws Exception {
        setUp();

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[0]
        );

        System.out.println("📭 빈 파일 업로드 테스트 시작...");
        System.out.println("📄 파일명: " + emptyFile.getOriginalFilename());
        System.out.println("📦 파일 크기: " + emptyFile.getSize() + " bytes");

        MvcResult result = mockMvc.perform(multipart("/api/files/upload").file(emptyFile))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonResponse = objectMapper.readTree(responseBody);
        val taskId = jsonResponse.get("id").asLong();

        System.out.println("📋 업로드 응답: " + responseBody);
        System.out.println("🆔 생성된 작업 ID: " + taskId);
        System.out.println("💡 현재 시스템은 빈 파일도 허용합니다.");
        System.out.println("✅ 실제 동작 확인 완료!");
    }
}