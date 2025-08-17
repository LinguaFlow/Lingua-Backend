package backend_lingua.linguas.member.enumerated;


import backend_lingua.linguas.util.exception.BusinessException;
import backend_lingua.linguas.util.http.HttpResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum MemberRole {

    MEMBER("MEMBER", "2"),

    ADMIN("ADMIN", "1");



    private final String role;
    private final String roleNumber;

    public static MemberRole byRole(String role) {
        return Arrays.stream(MemberRole.values())
                .filter(value -> value.getRole().equals(role))
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpResponse.FailureStatus.BAD_REQUEST));
    }

    public static MemberRole byRoleNumber(String roleNumber) {
        return Arrays.stream(MemberRole.values())
                .filter(value -> value.getRoleNumber().equals(roleNumber))
                .findAny()
                .orElseThrow(() -> new BusinessException(HttpResponse.FailureStatus.BAD_REQUEST));
    }
}
