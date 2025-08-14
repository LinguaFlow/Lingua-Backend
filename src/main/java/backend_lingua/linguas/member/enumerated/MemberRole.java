package backend_lingua.linguas.member.enumerated;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberRole {
    USER("ROLE_USER", "일반 사용자"),
    ADMIN("ROLE_ADMIN", "관리자"),
    OIDC_USER("ROLE_OIDC_USER", "OIDC 사용자");

    private final String key;
    private final String title;
}
