package backend_lingua.linguas.domain.oauth.enumerated;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ProviderTypeConverter implements AttributeConverter<ProviderType, String> {
    @Override
    public String convertToDatabaseColumn(ProviderType attribute) {
        return attribute.getProviderNumber();
    }

    @Override
    public ProviderType convertToEntityAttribute(String dbData) {
        return ProviderType.byProviderNumber(dbData);
    }
}