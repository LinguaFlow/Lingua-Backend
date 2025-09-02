package backend_lingua.linguas.infrastructure.sqs.config;

import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;

import java.time.Duration;

@Configuration
public class AwsSqsConfig {

    @Value("${ncp.cloud.aws.s3.region}")
    private String regionName;

    @Value("${ncp.cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${ncp.cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${queue.json-event-url}")
    private String queueUrl;

    // 리전 설정
    @Bean
    Region region() {
        return Region.of(regionName);
    }

    // 자격 증명 공급자 설정
    @Bean
    AwsCredentialsProvider awsCredentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey));
    }

    // SQS 비동기 클라이언트 설정
    @Bean
    SqsAsyncClient sqsAsyncClient(AwsCredentialsProvider credentialsProvider, Region region) {
        return SqsAsyncClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(region)
                .build();
    }

    // SQS 템플릿 설정 (메시지 전송용)
    @Bean
    SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
        return SqsTemplate.builder()
                .sqsAsyncClient(sqsAsyncClient)
                .build();
    }

    // SQS 리스너 컨테이너 팩토리 설정 (메시지 수신용)
    @Bean
    SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(SqsAsyncClient sqsAsyncClient) {
        return SqsMessageListenerContainerFactory.builder()
                .sqsAsyncClient(sqsAsyncClient)
                .configure(options -> options
                        .maxConcurrentMessages(10)          // 높은 병렬성
                        .maxMessagesPerPoll(10)
                        .acknowledgementMode(AcknowledgementMode.ON_SUCCESS)

                )
                .build();
    }

    // 큐 이름 추출 (URL에서)
    @Bean
    public String jsonEventQueueName() {
        // URL에서 큐 이름 추출 (마지막 '/' 이후의 문자열)
        return queueUrl.substring(queueUrl.lastIndexOf('/') + 1);
    }
}