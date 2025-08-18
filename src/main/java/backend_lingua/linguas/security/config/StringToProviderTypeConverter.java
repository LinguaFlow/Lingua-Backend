package backend_lingua.linguas.security.config;

import backend_lingua.linguas.oauth.enumerated.ProviderType;
import org.springframework.stereotype.Component;
import org.springframework.core.convert.converter.Converter;


@Component
public class StringToProviderTypeConverter implements Converter<String, ProviderType> {

    @Override
    public ProviderType convert(String source) {
        return ProviderType.byProviderName(source);
    }
}