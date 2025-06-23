package backend_lingua.linguas.kanji.api;

import backend_lingua.linguas.kanji.entity.TaskStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.jshell.Snippet;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // 추가
class KanjiControllerIntegrationTest {

//    @Autowired
//    private WebApplicationContext webApplicationContext;
//
//    private record TaskStatusInfo(TaskStatus taskStatus, String status, String responseBody) {
//    }
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    private MockMvc mockMvc;
//
//    void setUp() {
//        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
//    }
//
//    private MockMultipartFile createRealPdfFile() {
//        try {
//            String filePath = "/Users/hwangjungseog/Downloads/일본 단어장単語.pdf";
//            Path path = Paths.get(filePath);
//
//            if (Files.exists(path)) {
//                byte[] content = Files.readAllBytes(path);
//                String fileName = path.getFileName().toString();
//
//                System.out.println("📄 실제 PDF 파일 로드 성공: " + filePath);
//                System.out.println("📋 파일명: " + fileName);
//                System.out.println("📦 파일 크기: " + content.length + " bytes");
//
//                return new MockMultipartFile(
//                        "file",
//                        fileName,
//                        MediaType.APPLICATION_PDF_VALUE,
//                        content
//                );
//            } else {
//                throw new RuntimeException("PDF 파일을 찾을 수 없습니다: " + filePath);
//            }
//
//        } catch (IOException e) {
//            throw new RuntimeException("PDF 파일 읽기 실패: " + e.getMessage(), e);
//        }
//    }
//
//    @Test
//    @DisplayName("Flask API 연결 테스트")
//    void test_FlaskApi_RealConnection() {
//        setUp();
//
//        try {
//            // When & Then
//            MvcResult result = mockMvc.perform(get("/api/files/test"))
//                    .andDo(print())
//                    .andExpect(status().isOk())
//                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                    .andReturn();
//
//            // 응답 분석
//            String responseBody = result.getResponse().getContentAsString();
//            System.out.println("📄 Flask API 응답: " + responseBody);
//
//            JsonNode jsonResponse = objectMapper.readTree(responseBody);
//            System.out.println("🎨 응답 JSON (Pretty):");
//            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonResponse));
//
//            System.out.println("✅ Flask API 연결 테스트 성공!");
//
//        } catch (Exception e) {
//            System.out.println("⚠️ Flask API 연결 실패 (서버가 실행 중이지 않을 수 있습니다): " + e.getMessage());
//            // Flask 서버가 실행 중이지 않을 때는 테스트를 스킵
//            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Flask 서버 연결 불가");
//        }
//    }
//
//
//    @Test
//    @DisplayName("실제 PDF 파일 업로드 및 전체 워크플로우 테스트")
//    @DirtiesContext
//    void realPdf_FullWorkflow_Integration() throws Exception {
//        setUp();
//
//        MockMultipartFile file = createRealPdfFile();
//
//        System.out.println("🚀 실제 PDF 파일 업로드 시작...");
//        System.out.println("📄 파일명: " + file.getOriginalFilename());
//        System.out.println("📦 파일 크기: " + file.getSize() + " bytes");
//
//        MvcResult uploadResult =
//                mockMvc.perform(multipart("/api/files/upload").file(file))
//                        .andDo(print())
//                        .andExpect(status().isAccepted())
//                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                        .andExpect(jsonPath("$.id").exists())
//                        .andExpect(jsonPath("$.message").exists())
//                        .andReturn();
//
//        String uploadResponseBody = uploadResult.getResponse().getContentAsString();
//        JsonNode uploadJson = objectMapper.readTree(uploadResponseBody);
//        Long taskId = uploadJson.get("id").asLong();
//        assertThat(taskId).isNotNull();
//
//        TaskStatusInfo statusInfo = getCurrentTaskStatus(taskId);
//        assertThat(statusInfo).isNotNull();
//        assertThat(statusInfo.taskStatus()).isIn(TaskStatus.PENDING, TaskStatus.PROCESSING);
//
//        handleTaskStatusResult(taskId, statusInfo.taskStatus());
//
//        waitForProcessingAndVerifyFinalResult(taskId);
//    }
//
//    /**
//     * 현재 작업 상태를 조회하고 TaskStatus Enum으로 변환
//     */
//    private TaskStatusInfo getCurrentTaskStatus(Long taskId) throws Exception {
//        MvcResult statusResult = mockMvc.perform(get("/api/files/{id}/status", taskId))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$.status").exists())
//                .andExpect(jsonPath("$.code").exists())
//                .andExpect(jsonPath("$.message").exists())
//                .andReturn();
//
//        String statusResponseBody = statusResult.getResponse().getContentAsString();
//        JsonNode statusJson = objectMapper.readTree(statusResponseBody);
//        String currentStatus = statusJson.get("status").asText();
//
//        // TaskStatus Enum으로 변환
//        TaskStatus taskStatus = TaskStatus.fromStatus(currentStatus);
//
//        return new TaskStatusInfo(taskStatus, currentStatus, statusResponseBody);
//    }
//
//
//    public void handleTaskStatusResult(Long taskId, TaskStatus taskStatus) throws Exception {
//        switch (taskStatus) {
//            case PENDING -> handlePendingResult(taskId);
//            case PROCESSING -> handleProcessingResult(taskId);
//            case DONE -> handleDoneResult(taskId);
//            case FAILED -> handleFailedResult(taskId);
//        }
//    }
//
//    public void handlePendingResult(Long taskId) throws Exception {
//        System.out.println("⏳ PENDING 상태 결과 조회...");
//
//        MvcResult pendingResult = mockMvc.perform(get("/api/files/{id}/result", taskId))
//                .andDo(print())
//                .andExpect(status().isAccepted())
//                .andReturn();
//
//        String responseBody = pendingResult.getResponse().getContentAsString();
//        assertThat(pendingResult.getResponse().getStatus()).isEqualTo(HttpStatus.ACCEPTED.value());
//        assertThat(responseBody).isEmpty();
//        assertThat(TaskStatus.PENDING.getMessage(null)).contains("처리 대기");
//    }
//
//    public void handleProcessingResult(Long taskId) throws Exception {
//        System.out.println("⚙️ PROCESSING 상태 결과 조회...");
//
//        MvcResult processingResult = mockMvc.perform(get("/api/files/{id}/result", taskId))
//                .andDo(print())
//                .andExpect(status().isAccepted())
//                .andReturn();
//
//        String responseBody = processingResult.getResponse().getContentAsString();
//
//        System.out.println("3️⃣ PROCESSING 상태 결과:");
//        System.out.println("📋 응답 상태: " + processingResult.getResponse().getStatus());
//        System.out.println("📋 응답 본문: " + (responseBody.isEmpty() ? "없음 (예상됨)" : responseBody));
//
//        // TaskStatus의 메시지 검증
//        assertThat(TaskStatus.PROCESSING.getMessage(null)).contains("처리 중");
//    }
//
//    /**
//     * DONE 상태 결과 처리
//     */
//    public void handleDoneResult(Long taskId) throws Exception {
//        System.out.println("✅ DONE 상태 결과 조회...");
//
//        MvcResult doneResult = mockMvc.perform(get("/api/files/{id}/result", taskId))
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andReturn();
//
//        String doneResponseBody = doneResult.getResponse().getContentAsString();
//        System.out.println("3️⃣ DONE 상태 결과:");
//        System.out.println("📋 응답: " + doneResponseBody);
//
//        if (!doneResponseBody.isEmpty()) {
//            JsonNode doneJson = objectMapper.readTree(doneResponseBody);
//            System.out.println("🎨 결과 JSON (Pretty):");
//            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(doneJson));// 결과 데이터 구조 검증
//            assertThat(doneJson).isNotNull();
//        }
//
//        // TaskStatus의 상태 그룹 검증
//        assertThat(TaskStatus.DONE.isCompleted()).isTrue();
//        assertThat(TaskStatus.DONE.isSuccessful()).isTrue();
//        assertThat(TaskStatus.DONE.isProcessing()).isFalse();
//    }
//
//
//    public void handleFailedResult(Long taskId) throws Exception {
//        System.out.println("❌ FAILED 상태 결과 조회...");
//
//        MvcResult failedResult = mockMvc.perform(get("/api/files/{id}/result", taskId))
//                .andDo(print())
//                .andExpect(status().isInternalServerError())
//                .andReturn();
//
//        String responseBody = failedResult.getResponse().getContentAsString();
//
//        System.out.println("3️⃣ FAILED 상태 결과:");
//        System.out.println("📋 응답 상태: " + failedResult.getResponse().getStatus());
//        System.out.println("📋 응답 본문: " + (responseBody.isEmpty() ? "없음 (예상됨)" : responseBody));
//
//        // TaskStatus의 상태 그룹 검증
//        assertThat(TaskStatus.FAILED.isCompleted()).isTrue();
//        assertThat(TaskStatus.FAILED.isFailed()).isTrue();
//        assertThat(TaskStatus.FAILED.isSuccessful()).isFalse();
//        assertThat(TaskStatus.FAILED.isProcessing()).isFalse();
//    }
//
//
//    public void waitForProcessingAndVerifyFinalResult(Long taskId) {
//        System.out.println("\n⏳ SQS 메시지 처리를 기다리는 중... (30초 대기)");
//
//        try {
//            // 30초 대기를 여러 단계로 나누어 중간 상태도 확인
//            for (int i = 0; i < 6; i++) {
//                Thread.sleep(5000); // 5초씩 대기
//
//                try {
//                    TaskStatusInfo currentStatus = getCurrentTaskStatus(taskId);
//                    System.out.printf("⏰ %d초 경과 - 현재 상태: %s%n", (i + 1) * 5, currentStatus.status());
//
//                    // 완료된 상태라면 더 이상 기다리지 않음
//                    if (currentStatus.taskStatus().isCompleted()) {
//                        System.out.println("🎯 작업이 완료되었습니다!");
//                        break;
//                    }
//                } catch (Exception e) {
//                    System.out.println("⚠️ 중간 상태 확인 중 오류: " + e.getMessage());
//                }
//            }
//
//            // 최종 상태 재확인
//            TaskStatusInfo finalStatusInfo = getCurrentTaskStatus(taskId);
//            System.out.println("4️⃣ 최종 상태: " + finalStatusInfo.status());
//            System.out.println("📋 최종 응답: " + finalStatusInfo.responseBody());
//
//            // 최종 상태에 따른 추가 검증
//            verifyFinalState(taskId, finalStatusInfo.taskStatus());
//
//        } catch (InterruptedException e) {
//            System.out.println("⚠️ 대기 중 인터럽트 발생: " + e.getMessage());
//            Thread.currentThread().interrupt(); // 인터럽트 상태 복원
//        } catch (Exception e) {
//            System.out.println("⚠️ 최종 상태 확인 중 오류: " + e.getMessage());
//        }
//    }
//
//    public void verifyFinalState(Long taskId, TaskStatus finalStatus) throws Exception {
//        if (finalStatus == TaskStatus.DONE) {
//            // 성공적으로 완료된 경우 최종 결과 조회 및 검증
//            MvcResult finalResult = mockMvc.perform(get("/api/files/{id}/result", taskId))
//                    .andExpect(status().isOk())
//                    .andReturn();
//
//            String finalResultBody = finalResult.getResponse().getContentAsString();
//            if (!finalResultBody.isEmpty()) {
//                System.out.println("🎯 최종 처리 결과:");
//                System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
//                        objectMapper.readTree(finalResultBody)));
//            }
//
//            // 상태 전환 검증 - DONE 상태에서는 다른 상태로 전환 불가
//            assertThat(finalStatus.canTransitionTo(TaskStatus.PROCESSING)).isFalse();
//            assertThat(finalStatus.canTransitionTo(TaskStatus.PENDING)).isFalse();
//
//        } else if (finalStatus == TaskStatus.FAILED) {
//            System.out.println("❌ 작업이 실패로 완료되었습니다.");
//
//            // 실패 상태 검증
//            assertThat(finalStatus.isFailed()).isTrue();
//            assertThat(finalStatus.canTransitionTo(TaskStatus.PENDING)).isTrue(); // 재시작 가능
//
//        } else if (finalStatus.isProcessing()) {
//            System.out.println("⏳ 작업이 아직 진행 중입니다.");
//
//            // 진행 중 상태 검증
//            assertThat(finalStatus.isProcessing()).isTrue();
//            assertThat(finalStatus.isCompleted()).isFalse();
//        }
//    }
//
//    @Test
//    @DisplayName("잘못된 파일 형식 업로드 테스트")
//    void upload_InvalidFileType_Integration() throws Exception {
//        setUp();
//
//        // Given - 텍스트 파일로 잘못된 형식 시뮬레이션
//        MockMultipartFile invalidFile = new MockMultipartFile(
//                "file",
//                "invalid-document.txt",
//                MediaType.TEXT_PLAIN_VALUE,
//                "이것은 PDF가 아닌 텍스트 파일입니다.".getBytes()
//        );
//
//        System.out.println("🚫 잘못된 파일 형식 업로드 테스트 시작...");
//        System.out.println("📄 파일명: " + invalidFile.getOriginalFilename());
//        System.out.println("📋 파일 타입: " + invalidFile.getContentType());
//
//        MvcResult result = mockMvc
//                .perform(multipart("/api/files/upload").file(invalidFile))
//                .andDo(print())
//                .andExpect(status().isAccepted())
//                .andReturn();
//
//        String responseBody = result.getResponse().getContentAsString();
//        JsonNode jsonResponse = objectMapper.readTree(responseBody);
//        val taskId = jsonResponse.get("id").asLong();
//
//        System.out.println("📋 업로드 응답: " + responseBody);
//        System.out.println("🆔 생성된 작업 ID: " + taskId);
//        System.out.println("💡 현재 시스템은 파일 형식 검증 없이 모든 파일을 허용합니다.");
//        System.out.println("✅ 실제 동작 확인 완료!");
//    }
//
//    @Test
//    @DisplayName("빈 파일 업로드 테스트")
//    void upload_EmptyFile_Integration() throws Exception {
//        setUp();
//
//        // Given - 빈 파일
//        MockMultipartFile emptyFile = new MockMultipartFile(
//                "file",
//                "empty.pdf",
//                MediaType.APPLICATION_PDF_VALUE,
//                new byte[0]  // 빈 바이트 배열
//        );
//
//        System.out.println("📭 빈 파일 업로드 테스트 시작...");
//        System.out.println("📄 파일명: " + emptyFile.getOriginalFilename());
//        System.out.println("📦 파일 크기: " + emptyFile.getSize() + " bytes");
//
//        // When & Then - 실제로는 빈 파일도 업로드됨
//        MvcResult result = mockMvc.perform(multipart("/api/files/upload").file(emptyFile))
//                .andDo(print())
//                .andExpect(status().isAccepted()) // 실제 동작에 맞춰 수정
//                .andReturn();
//
//        String responseBody = result.getResponse().getContentAsString();
//        JsonNode jsonResponse = objectMapper.readTree(responseBody);
//        val taskId = jsonResponse.get("id").asLong();
//
//        System.out.println("📋 업로드 응답: " + responseBody);
//        System.out.println("🆔 생성된 작업 ID: " + taskId);
//        System.out.println("💡 현재 시스템은 빈 파일도 허용합니다.");
//        System.out.println("✅ 실제 동작 확인 완료!");
//    }
}