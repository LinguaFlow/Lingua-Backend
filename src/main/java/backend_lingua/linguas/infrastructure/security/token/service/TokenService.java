package backend_lingua.linguas.infrastructure.security.token.service;

import backend_lingua.linguas.domain.member.entity.Member;
import backend_lingua.linguas.infrastructure.security.token.entity.RefreshToken;
import org.springframework.security.core.Authentication;

import java.util.Date;

public interface TokenService {
    RefreshToken createRefreshToken(String token, Date expiryDate, Authentication authentication);

    void deleteRefreshToken(String email);

    void deleteRefreshTokenByUser(Member member);  // 추가

}