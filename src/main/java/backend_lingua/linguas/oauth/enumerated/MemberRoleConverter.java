package backend_lingua.linguas.oauth.enumerated;

import backend_lingua.linguas.member.enumerated.MemberRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MemberRoleConverter implements AttributeConverter<MemberRole, String> {
    @Override
    public String convertToDatabaseColumn(MemberRole attribute) {
        return attribute.getRoleNumber();
    }

    @Override
    public MemberRole convertToEntityAttribute(String dbData) {
        return MemberRole.byRoleNumber(dbData);
    }
}