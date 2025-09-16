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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("멀티유저 파일 동시 업로드 통합 테스트")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FileMultithreadingUploadTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VocabularyWordRepository vocabularyWordRepository;

    private static final int CONCURRENT_USERS = 10;
    private static final int THREAD_POOL_SIZE = 10;
    private static final int UPLOAD_TIMEOUT_SECONDS = 60;
    private static final int RESULT_TIMEOUT_SECONDS = 5;

    private List<Member> testMembers = new ArrayList<>();

    private record UploadResult(Long taskId, String message, Long memberId, int userIndex) {}

    private record ProcessingResult(Long taskId, Long memberId, int userIndex,
                                    TaskStatus finalStatus, String resultData) {}

    @BeforeEach
    void beforeEach() {
        // Given - 10명의 테스트 사용자 생성
        for (int i = 1; i <= CONCURRENT_USERS; i++) {
            Member mockMember = Member.builder()
                    .email("test" + i + "@example.com")
                    .name("테스트 사용자 " + i)
                    .role(MemberRole.MEMBER)
                    .provider(ProviderType.KAKAO)
                    .providerId("kakao_" + i)
                    .build();

            testMembers.add(mockMember);
        }

        memberRepository.saveAll(testMembers);

        // 사용자가 제대로 생성되었는지 확인
        System.out.println("Created " + testMembers.size() + " test users");
        testMembers.forEach(member ->
                System.out.println("User " + member.getId() + ": " + member.getEmail())
        );
    }

    @AfterEach
    void afterEach() {
        vocabularyWordRepository.deleteAll();
        memberRepository.deleteAll();
        testMembers.clear();
    }

    private MockMultipartFile createRealPdfFile(int userIndex) throws IOException {
        String filePath = "/Users/hwangjungseog/Downloads/Test Data/단어.pdf";
        Path path = Paths.get(filePath);

        byte[] content = Files.readAllBytes(path);
        // 각 사용자마다 다른 파일명으로 업로드 (충돌 방지)
        String fileName = "단어_user" + userIndex + ".pdf";

        return new MockMultipartFile(
                "file",
                fileName,
                MediaType.APPLICATION_PDF_VALUE,
                content
        );
    }

    @Test
    @DisplayName("10명의 사용자가 동시에 파일 업로드 및 결과 조회 완료까지 테스트")
    void multiUser_Concurrent_Upload_And_Result_Test() throws Exception {
        // Given - 동시 요청을 위한 스레드 풀과 동기화 도구 준비
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CountDownLatch startLatch = new CountDownLatch(1); // 모든 스레드가 동시에 시작하도록
        CountDownLatch completeLatch = new CountDownLatch(CONCURRENT_USERS);
        List<Future<UploadResult>> uploadFutures = new ArrayList<>();

        // When - 10명의 사용자가 동시에 업로드 요청 실행
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final int userIndex = i;
            final Member member = testMembers.get(i);

            Callable<UploadResult> uploadTask = () -> {
                try {
                    // 모든 스레드가 준비될 때까지 대기
                    startLatch.await();

                    // 각 스레드에서 해당 사용자 사용
                    UserPrincipal userPrincipal = UserPrincipal.create(member);

                    // 각 사용자별로 다른 파일 준비
                    MockMultipartFile file = createRealPdfFile(userIndex + 1);

                    MvcResult result = mockMvc.perform(multipart("/api/files/upload")
                                    .file(file)
                                    .with(user(userPrincipal))
                                    .with(request -> {
                                        UsernamePasswordAuthenticationToken authentication =
                                                new UsernamePasswordAuthenticationToken(
                                                        userPrincipal,
                                                        null,
                                                        userPrincipal.getAuthorities()
                                                );
                                        SecurityContextHolder.getContext().setAuthentication(authentication);
                                        return request;
                                    })
                            )
                            .andExpect(status().isAccepted())
                            .andExpect(jsonPath("$.taskId").exists())
                            .andExpect(jsonPath("$.message").exists())
                            .andReturn();

                    // 응답 파싱
                    String responseBody = result.getResponse().getContentAsString();
                    JsonNode jsonResponse = objectMapper.readTree(responseBody);
                    Long taskId = jsonResponse.get("taskId").asLong();
                    String message = jsonResponse.get("message").asText();

                    completeLatch.countDown();
                    return new UploadResult(taskId, message, member.getId(), userIndex + 1);

                } catch (Exception e) {
                    completeLatch.countDown();
                    e.printStackTrace();
                    throw new RuntimeException("사용자 " + (userIndex + 1) + " 업로드 실패", e);
                }
            };

            uploadFutures.add(executorService.submit(uploadTask));
        }

        // 모든 스레드를 동시에 시작
        startLatch.countDown();

        // 모든 업로드 요청 완료 대기
        boolean completed = completeLatch.await(UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then - 업로드 결과 검증
        assertThat(completed).isTrue();

        List<UploadResult> uploadResults = collectUploadResults(uploadFutures);
        verifyMultiUserUploadResults(uploadResults);
        verifyTaskUniqueness(uploadResults);

        // 추가: 각 작업의 처리 완료까지 대기하고 결과 조회
        List<ProcessingResult> processingResults = waitForAllTasksAndGetResults(uploadResults);
        verifyFinalResults(processingResults);

        printCompleteSummary(uploadResults, processingResults);
    }

    /**
     * 모든 작업이 완료될 때까지 대기하고 결과를 조회
     */
    private List<ProcessingResult> waitForAllTasksAndGetResults(List<UploadResult> uploadResults)
            throws Exception {
        List<ProcessingResult> processingResults = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        System.out.println("\n========== 모든 작업 완료 대기 시작 ==========");
        System.out.println("대기할 작업 수: " + uploadResults.size());

        // 모든 작업이 DONE 또는 FAILED가 될 때까지 반복
        int loopCount = 0;
        while (true) {
            loopCount++;
            int doneCount = 0;
            int failedCount = 0;
            int processingCount = 0;
            int pendingCount = 0;

            // 각 작업의 상태 확인
            for (UploadResult uploadResult : uploadResults) {
                try {
                    TaskStatusInfo status = getCurrentTaskStatus(uploadResult.taskId());

                    switch (status.taskStatus()) {
                        case DONE -> doneCount++;
                        case FAILED -> failedCount++;
                        case PROCESSING -> processingCount++;
                        case PENDING -> pendingCount++;
                    }
                } catch (Exception e) {
                    // API 호출 실패 시 PROCESSING으로 간주
                    System.err.println("Task " + uploadResult.taskId() + " 상태 확인 실패: " + e.getMessage());
                    processingCount++;
                }
            }

            // 진행 상황 출력
            long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
            System.out.println(String.format("[Loop %d, %3d초] DONE: %d, PROCESSING: %d, PENDING: %d, FAILED: %d",
                    loopCount, elapsedSeconds, doneCount, processingCount, pendingCount, failedCount));

            // 모든 작업이 완료(DONE 또는 FAILED)되었는지 확인
            if (doneCount + failedCount == uploadResults.size()) {
                System.out.println("✅ 모든 작업 완료! (DONE: " + doneCount + ", FAILED: " + failedCount + ")");
                break;
            }

            // 너무 오래 걸리면 경고 (하지만 계속 대기)
            if (elapsedSeconds > 120) {
                System.out.println("⚠️ 2분 이상 대기 중... 아직 " +
                        (processingCount + pendingCount) + "개 작업 진행 중");
            }

            // 5초 대기
            Thread.sleep(5000);
        }

        // 최종 결과 수집
        for (UploadResult uploadResult : uploadResults) {
            try {
                TaskStatusInfo finalStatus = getCurrentTaskStatus(uploadResult.taskId());
                String resultData = null;

                if (finalStatus.taskStatus() == TaskStatus.DONE) {
                    resultData = getFinalResult(uploadResult.taskId());
                }

                processingResults.add(new ProcessingResult(
                        uploadResult.taskId(),
                        uploadResult.memberId(),
                        uploadResult.userIndex(),
                        finalStatus.taskStatus(),
                        resultData
                ));
            } catch (Exception e) {
                // 최종 결과 수집 실패 시 FAILED로 처리
                processingResults.add(new ProcessingResult(
                        uploadResult.taskId(),
                        uploadResult.memberId(),
                        uploadResult.userIndex(),
                        TaskStatus.FAILED,
                        null
                ));
            }
        }

        return processingResults;
    }



    /**
     * 최종 결과 조회
     */
    private String getFinalResult(Long taskId) throws Exception {
        MvcResult finalResult = mockMvc.perform(get("/api/files/{id}/result", taskId))
                .andExpect(status().isOk())
                .andReturn();

        String resultBody = finalResult.getResponse().getContentAsString();
        assertThat(resultBody).isNotEmpty();

        return resultBody;
    }

    /**
     * 현재 작업 상태 조회
     */
    private TaskStatusInfo getCurrentTaskStatus(Long taskId) throws Exception {
        MvcResult statusResult = mockMvc.perform(get("/api/files/{id}/status", taskId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").exists())
                .andReturn();

        String statusResponseBody = statusResult.getResponse().getContentAsString();
        JsonNode statusJson = objectMapper.readTree(statusResponseBody);
        String currentStatus = statusJson.get("status").asText();
        TaskStatus taskStatus = TaskStatus.fromStatus(currentStatus);

        return new TaskStatusInfo(taskStatus, currentStatus, statusResponseBody);
    }

    /**
     * 업로드 결과 수집
     */
    private List<UploadResult> collectUploadResults(List<Future<UploadResult>> futures) {
        List<UploadResult> results = new ArrayList<>();

        for (int i = 0; i < futures.size(); i++) {
            try {
                Future<UploadResult> future = futures.get(i);
                UploadResult result = future.get(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                results.add(result);
                System.out.println("사용자 " + result.userIndex() + " 업로드 성공 - Task ID: " + result.taskId());
            } catch (TimeoutException e) {
                System.err.println("사용자 " + (i + 1) + " 업로드 타임아웃");
            } catch (Exception e) {
                System.err.println("사용자 " + (i + 1) + " 업로드 실패: " + e.getMessage());
                if (e.getCause() != null) {
                    System.err.println("  원인: " + e.getCause().getMessage());
                }
            }
        }

        return results;
    }

    /**
     * 멀티유저 업로드 결과 검증
     */
    private void verifyMultiUserUploadResults(List<UploadResult> uploadResults) {
        Map<Long, List<UploadResult>> resultsByMember = uploadResults.stream()
                .collect(Collectors.groupingBy(UploadResult::memberId));

        assertThat(uploadResults.size()).isEqualTo(CONCURRENT_USERS);
        assertThat(resultsByMember.keySet().size()).isEqualTo(CONCURRENT_USERS);

        // 각 사용자가 정확히 1개의 파일을 업로드했는지 확인
        resultsByMember.forEach((memberId, results) -> {
            assertThat(results.size()).isEqualTo(1);
        });
    }

    /**
     * Task ID 고유성 검증
     */
    private void verifyTaskUniqueness(List<UploadResult> uploadResults) {
        Set<Long> uniqueTaskIds = new HashSet<>();
        Set<Long> uniqueMemberIds = new HashSet<>();

        for (UploadResult result : uploadResults) {
            uniqueTaskIds.add(result.taskId());
            uniqueMemberIds.add(result.memberId());
        }

        assertThat(uniqueTaskIds.size()).isEqualTo(uploadResults.size());
        System.out.println("총 " + uniqueTaskIds.size() + "개의 고유한 Task ID 생성됨");
        System.out.println("총 " + uniqueMemberIds.size() + "명의 고유한 사용자가 업로드함");
    }

    /**
     * 최종 처리 결과 검증
     */
    private void verifyFinalResults(List<ProcessingResult> processingResults) {
        // 성공/실패 카운트
        Map<TaskStatus, Long> statusCount = processingResults.stream()
                .collect(Collectors.groupingBy(
                        ProcessingResult::finalStatus,
                        Collectors.counting()
                ));

        long successCount = statusCount.getOrDefault(TaskStatus.DONE, 0L);
        long failCount = statusCount.getOrDefault(TaskStatus.FAILED, 0L);

        System.out.println("\n최종 처리 결과:");
        System.out.println("  성공: " + successCount + "개");
        System.out.println("  실패: " + failCount + "개");

        // 성공한 작업들의 결과 데이터 검증
        processingResults.stream()
                .filter(result -> result.finalStatus() == TaskStatus.DONE)
                .forEach(result -> {
                    assertThat(result.resultData()).isNotNull();
                    assertThat(result.resultData()).isNotEmpty();
                });
    }

    /**
     * 전체 요약 출력
     */
    private void printCompleteSummary(List<UploadResult> uploadResults,
                                      List<ProcessingResult> processingResults) {
        System.out.println("\n========== 전체 처리 요약 ==========");
        System.out.println("총 사용자 수: " + CONCURRENT_USERS);
        System.out.println("업로드 성공: " + uploadResults.size());
        System.out.println("업로드 실패: " + (CONCURRENT_USERS - uploadResults.size()));

        Map<TaskStatus, Long> finalStatusCount = processingResults.stream()
                .collect(Collectors.groupingBy(
                        ProcessingResult::finalStatus,
                        Collectors.counting()
                ));

        System.out.println("\n최종 처리 상태:");
        finalStatusCount.forEach((status, count) ->
                System.out.println("  " + status + ": " + count + "개")
        );

        if (!processingResults.isEmpty()) {
            System.out.println("\n처리 결과 상세:");
            processingResults.forEach(result -> {
                String resultInfo = result.finalStatus() == TaskStatus.DONE
                        ? " - 결과 크기: " + (result.resultData() != null ? result.resultData().length() : 0) + " bytes"
                        : " - 실패";
                System.out.println(String.format("  사용자 %d (ID: %d) -> Task ID: %d, 상태: %s%s",
                        result.userIndex(), result.memberId(), result.taskId(),
                        result.finalStatus(), resultInfo));
            });
        }
        System.out.println("====================================\n");
    }
}