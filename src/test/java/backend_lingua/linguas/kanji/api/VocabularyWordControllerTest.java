package backend_lingua.linguas.kanji.api;

import backend_lingua.linguas.domain.member.entity.Member;
import backend_lingua.linguas.domain.member.enumerated.MemberRole;
import backend_lingua.linguas.domain.member.repository.MemberRepository;
import backend_lingua.linguas.domain.oauth.enumerated.ProviderType;
import backend_lingua.linguas.domain.vocabulary.dto.UploadStatusMessage;
import backend_lingua.linguas.domain.vocabulary.enumerated.TaskStatus;
import backend_lingua.linguas.domain.vocabulary.repository.VocabularyWordRepository;
import backend_lingua.linguas.infrastructure.security.principal.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.*;
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

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private CompletableFuture<UploadStatusMessage> statusMessageFuture;

    private MockMultipartFile createRealPdfFile() {
        try {
            String filePath = "/Users/hwangjungseog/Downloads/Test Data/테스트.pdf";
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

        // WebSocket 클라이언트 설정
        setupWebSocketClient();
    }

    @AfterEach
    void afterEach() {
        // WebSocket 연결 종료
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }

        vocabularyWordRepository.deleteAll();
        memberRepository.deleteAll();
    }

    private void setupWebSocketClient() {
        // SockJS를 사용한 WebSocket 클라이언트 설정
        List<Transport> transports = List.of(
                new WebSocketTransport(new StandardWebSocketClient())
        );

        SockJsClient sockJsClient = new SockJsClient(transports);
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    private StompSession connectWebSocket() {
        try {
            // 변경: /ws -> /ws-upload
            String url = String.format("ws://localhost:%d/ws", port);

            StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    System.out.println("WebSocket 연결 성공!");
                }

                @Override
                public void handleException(StompSession session, StompCommand command,
                                            StompHeaders headers, byte[] payload, Throwable exception) {
                    System.err.println("WebSocket 에러: " + exception.getMessage());
                }
            };

            return stompClient.connectAsync(url, sessionHandler).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("WebSocket 연결 실패, 폴링 모드로 전환: " + e.getMessage());
            return null;
        }
    }

    private CompletableFuture<UploadStatusMessage> subscribeToTaskStatus(Long taskId) {
        CompletableFuture<UploadStatusMessage> future = new CompletableFuture<>();

        String destination = "/topic/upload/" + taskId;
        stompSession.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return UploadStatusMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                UploadStatusMessage message = (UploadStatusMessage) payload;
                System.out.println("WebSocket 메시지 수신 - Task ID: " + message.getTaskId()
                        + ", Status: " + message.getStatus());

                // DONE 또는 FAILED 상태일 때 Future 완료
                if (message.getStatus() == TaskStatus.DONE ||
                        message.getStatus() == TaskStatus.FAILED) {
                    future.complete(message);
                }
            }
        });

        return future;
    }

    @Test
    @DisplayName("실제 PDF 파일 업로드 및 전체 워크플로우 테스트")
    void realPdf_FullWorkflow_Integration() throws Exception {
        // Given - 테스트 사용자 및 PDF 파일 준비
        Member mockMember = memberRepository.findByEmail("test@example.com")
                .orElseThrow(() -> new RuntimeException("테스트 Member를 찾을 수 없습니다"));

        UserPrincipal userPrincipal = UserPrincipal.create(mockMember);

        MockMultipartFile file = createRealPdfFile();

        // WebSocket 연결
        stompSession = connectWebSocket();

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

        int statusCode = uploadResult.getResponse().getStatus();
        String responseBody = uploadResult.getResponse().getContentAsString();

        assertThat(statusCode).isEqualTo(HttpStatus.ACCEPTED.value());
        assertThat(!responseBody.isEmpty()).isTrue();

        JsonNode uploadJson = objectMapper.readTree(responseBody);
        Long taskId = uploadJson.get("taskId").asLong();
        assertThat(taskId).isNotNull();

        // WebSocket 구독 설정
        if (stompSession != null && stompSession.isConnected()) {
            statusMessageFuture = subscribeToTaskStatus(taskId);
        }

        processTaskStatus(taskId);
    }

    private void processTaskStatus(Long taskId) throws Exception {
        // When - 현재 작업 상태 조회
        TaskStatusInfo statusInfo = getCurrentTaskStatus(taskId);

        // Then - 상태 검증 및 처리
        assertThat(statusInfo).isNotNull();
        assertThat(statusInfo.taskStatus()).isIn(TaskStatus.PENDING, TaskStatus.PROCESSING);

        handleTaskStatusResult(taskId, statusInfo.taskStatus());

        // WebSocket을 통한 실시간 상태 업데이트 대기 (기존 폴링 대신)
        waitForProcessingWithWebSocket(taskId);
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

    // WebSocket을 사용한 새로운 대기 메소드
    public void waitForProcessingWithWebSocket(Long taskId) {
        try {
            // WebSocket 연결이 성공한 경우에만
            if (statusMessageFuture != null) {
                UploadStatusMessage finalStatus = statusMessageFuture.get(5, TimeUnit.MINUTES);

                if (finalStatus.getStatus() == TaskStatus.DONE) {
                    verifyFinalState(taskId);
                } else if (finalStatus.getStatus() == TaskStatus.FAILED) {
                    throw new AssertionError("작업 처리 실패");
                }
            } else {
                // WebSocket 연결 실패 시 바로 폴링 방식 사용
                System.out.println("WebSocket 연결 불가, 폴링 방식 사용");
                waitForProcessingAndVerifyFinalResult(taskId);
            }
        } catch (TimeoutException e) {
            System.out.println("WebSocket 타임아웃, 폴링 방식으로 전환");
            waitForProcessingAndVerifyFinalResult(taskId);
        } catch (Exception e) {
            System.out.println("WebSocket 처리 중 오류, 폴링 방식으로 전환: " + e.getMessage());
            waitForProcessingAndVerifyFinalResult(taskId);
        }
    }

    // 기존 폴링 메소드 (폴백용으로 유지)
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

                Thread.sleep(10000);  // 10초 대기 (기존 30초에서 단축)
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
}