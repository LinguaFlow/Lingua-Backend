package backend_lingua.linguas.infrastructure.s3.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ncp.cloud.aws")
public class S3Properties {

    private S3 s3;
    private Credentials credentials;

    @Getter
    @Setter
    public static class S3 {
        String bucket;
        String endpoint;
        String region;
    }

    @Getter
    @Setter
    public static class Credentials {
        String accessKey;
        String secretKey;
    }
}