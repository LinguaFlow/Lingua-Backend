package backend_lingua.linguas.sqs.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;

@Configuration
public class SqsDisableConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.cloud.aws.sqs.enabled", havingValue = "false", matchIfMissing = false)
    public SqsMessageListenerContainer<?> sqsMessageListenerContainer() {
        return null;
    }
}