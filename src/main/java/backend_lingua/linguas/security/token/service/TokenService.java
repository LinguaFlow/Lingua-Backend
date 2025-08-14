package backend_lingua.linguas.security.token.service;

import backend_lingua.linguas.member.entity.Member;
import backend_lingua.linguas.security.token.dto.RefreshToken;
import org.springframework.security.core.Authentication;

import java.util.Date;

public interface TokenService {
    RefreshToken createRefreshToken(String token, Date expiryDate, Authentication authentication);
    RefreshToken findRefreshToken(String refreshToken);
    void deleteRefreshToken(String email);
    void deleteRefreshTokenByUser(Member member);  // 추가
    boolean isTokenExpired(RefreshToken token);
}