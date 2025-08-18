package backend_lingua.linguas.infrastructure.security.principal;

import backend_lingua.linguas.domain.member.entity.Member;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPrincipal implements OAuth2User, UserDetails {

    private Member member;
    private Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;

    public static UserPrincipal create(Member member) {
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + member.getRole().name())
        );

        return UserPrincipal.builder()
                .member(member)
                .authorities(authorities)
                .build();
    }

    public static UserPrincipal create(Member member, Map<String, Object> attributes) {
        UserPrincipal userPrincipal = UserPrincipal.create(member);

        return UserPrincipal.builder()
                .member(userPrincipal.getMember())
                .authorities(userPrincipal.getAuthorities())
                .attributes(attributes)
                .build();
    }

    // OAuth2User 구현
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return member.getName();
    }

    // UserDetails 구현
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {
        return member.getEmail();
    }

    @Override
    public String getPassword() {
        return null;  // OAuth2 로그인은 비밀번호 사용 안함
    }
}