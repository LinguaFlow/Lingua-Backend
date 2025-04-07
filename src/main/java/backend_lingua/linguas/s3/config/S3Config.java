package backend_lingua.linguas.s3.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final S3Properties properties;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.getCredentials().getAccessKey(),
                properties.getCredentials().getSecretKey()
        );

        return S3Client.builder()
                // 엔드포인트 URL 설정 (ex. http://localhost:9000 과 같이 로컬 또는 커스텀 엔드포인트)
                .endpointOverride(URI.create(properties.getS3().getEndpoint()))
                // AWS 리전 설정
                .region(Region.of(properties.getS3().getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}
