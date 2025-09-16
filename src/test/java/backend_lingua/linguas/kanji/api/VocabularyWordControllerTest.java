package backend_lingua.linguas.kanji.api;

import backend_lingua.linguas.domain.member.entity.Member;
import backend_lingua.linguas.domain.member.enumerated.MemberRole;
import backend_lingua.linguas.domain.member.repository.MemberRepository;
import backend_lingua.linguas.domain.oauth.enumerated.ProviderType;
import backend_lingua.linguas.domain.vocabulary.enumerated.TaskStatus;
import backend_lingua.linguas.domain.vocabulary.repository.VocabularyWordRepository;
import backend_lingua.linguas.infrastructure.security.principal.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.annotation.DirtiesContext;

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
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("VocabularyWordController 통합 테스트")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class VocabularyWordControllerTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VocabularyWordRepository vocabularyWordRepository;

    private MockMultipartFile createRealPdfFile() {
        try {
            String filePath = "/Users/hwangjungseog/Downloads/Test Data/테스트.pdf";
            Path path = Paths.get(filePath);

            byte[] content = Files.readAllBytes(path);
            String fileName = path.getFileName().toString();

            return new MockMultipartFile(
                    "file",
                    fileName,
                    MediaType.APPLICATION_PDF_VALUE,
                    content);

        } catch (IOException e) {
            throw new RuntimeException("PDF 파일 읽기 실패: " + e.getMessage(), e);
        }
    }

    @BeforeEach
    void beforeEach() {
        Member mockMember = Member.builder()
                .email("test@example.com")
                .name("테스트 사용자")
                .role(MemberRole.MEMBER)
                .provider(ProviderType.KAKAO)
                .build();

        memberRepository.save(mockMember);
    }

    @AfterEach
    void afterEach() {
        vocabularyWordRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("실제 PDF 파일 업로드 및 전체 워크플로우 테스트")
    void realPdf_FullWorkflow_Integration() throws Exception {
        // Given - 테스트 사용자 및 PDF 파일 준비
        Member mockMember = memberRepository.findByEmail("test@example.com")
                .orElseThrow(() -> new RuntimeException("테스트 Member를 찾을 수 없습니다"));

        UserPrincipal userPrincipal = UserPrincipal.create(mockMember);

        MockMultipartFile file = createRealPdfFile();

        // When - PDF 파일 업로드
        MvcResult uploadResult = mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .with(user(userPrincipal))
                        .with(request -> {
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userPrincipal,
                                    null,
                                    userPrincipal.getAuthorities()
                            );
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            return request;
                        })
                )
                .andDo(print())
                .andReturn();

        // Then - 업로드 응답 검증 및 작업 상태 처리
        int statusCode = uploadResult.getResponse().getStatus();
        String responseBody = uploadResult.getResponse().getContentAsString();

        assertThat(statusCode).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(!responseBody.isEmpty()).isTrue();

        JsonNode uploadJson = objectMapper.readTree(responseBody);
        Long taskId = uploadJson.get("taskId").asLong();
        assertThat(taskId).isNotNull();

        processTaskStatus(taskId);
    }

    private void processTaskStatus(Long taskId) throws Exception {
        // When - 현재 작업 상태 조회
        TaskStatusInfo statusInfo = getCurrentTaskStatus(taskId);

        // Then - 상태 검증 및 처리
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
        // When - PENDING 상태에서 결과 조회
        MvcResult pendingResult = mockMvc.perform(get("/api/files/{id}/result", taskId))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then - 응답 코드 검증
        int status = pendingResult.getResponse().getStatus();
        assertThat(status).isIn(HttpStatus.ACCEPTED.value(), HttpStatus.BAD_REQUEST.value());
    }

    public void handleProcessingResult(Long taskId) throws Exception {
        // When - PROCESSING 상태에서 결과 조회
        MvcResult processingResult = mockMvc.perform(get("/api/files/{id}/result", taskId))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andReturn();

        // Then - 상태 검증
        assertThat(TaskStatus.PROCESSING).isNotNull();
    }

    public void handleDoneResult(Long taskId) throws Exception {
        // When - DONE 상태에서 결과 조회
        MvcResult doneResult = mockMvc.perform(get("/api/files/{id}/result", taskId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then - 결과 검증
        String doneResponseBody = doneResult.getResponse().getContentAsString();
        if (!doneResponseBody.isEmpty()) {
            JsonNode doneJson = objectMapper.readTree(doneResponseBody);
            assertThat(doneJson).isNotNull();
        }
    }

    public void waitForProcessingAndVerifyFinalResult(Long taskId) {
        try {
            while (true) {
                // When - 작업 상태 확인
                TaskStatusInfo currentStatus = getCurrentTaskStatus(taskId);

                // Then - 상태에 따른 처리
                if (currentStatus.taskStatus() == TaskStatus.DONE) {
                    verifyFinalState(taskId);
                    return;
                }

                if (currentStatus.taskStatus() == TaskStatus.FAILED) {
                    throw new AssertionError("작업 처리 실패");
                }

                Thread.sleep(10000);
            }
        } catch (Exception e) {
            throw new RuntimeException("테스트 실행 중 오류", e);
        }
    }

    public void verifyFinalState(Long taskId) throws Exception {
        // When - 최종 결과 조회
        MvcResult finalResult = mockMvc.perform(get("/api/files/{id}/result", taskId))
                .andExpect(status().isOk())
                .andReturn();

        // Then - 최종 결과 검증
        String finalResultBody = finalResult.getResponse().getContentAsString();
        assertThat(finalResultBody).isNotEmpty();
    }

    public void handleFailedResult(Long taskId) throws Exception {
        // When - FAILED 상태에서 결과 조회
        MvcResult failedResult = mockMvc.perform(get("/api/files/{id}/result", taskId))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andReturn();

        // Then - 실패 상태 확인
        assertThat(failedResult.getResponse().getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    @DisplayName("잘못된 파일 형식 업로드 테스트")
    void upload_InvalidFileType_Integration() throws Exception {
        // Given - 잘못된 형식의 파일 준비
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "invalid-document.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "이것은 PDF가 아닌 텍스트 파일입니다.".getBytes()
        );

        // When - 파일 업로드
        MvcResult result = mockMvc.perform(multipart("/api/files/upload").file(invalidFile))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andReturn();

        // Then - 응답 검증
        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonResponse = objectMapper.readTree(responseBody);
        long taskId = jsonResponse.get("id").asLong();
        assertThat(taskId).isNotNull();
    }

    @Test
    @DisplayName("빈 파일 업로드 테스트")
    void upload_EmptyFile_Integration() throws Exception {
        // Given - 빈 파일 준비
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[0]
        );

        // When - 빈 파일 업로드
        MvcResult result = mockMvc.perform(multipart("/api/files/upload").file(emptyFile))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andReturn();

        // Then - 응답 검증
        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonResponse = objectMapper.readTree(responseBody);
        long taskId = jsonResponse.get("id").asLong();
        assertThat(taskId).isNotNull();
    }
}