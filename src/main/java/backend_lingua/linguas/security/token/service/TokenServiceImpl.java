package backend_lingua.linguas.security.token.service;

import backend_lingua.linguas.member.entity.Member;
import backend_lingua.linguas.member.repository.MemberRepository;
import backend_lingua.linguas.security.token.dto.RefreshToken;
import backend_lingua.linguas.security.token.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenServiceImpl implements TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository userRepository;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(String token, Date expiryDate, Authentication authentication) {
        String email = authentication.getName();
        Member member = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        // 기존 토큰 삭제
        refreshTokenRepository.findByMemberId(member.getId())
                .ifPresent(refreshTokenRepository::delete);

        // 새 토큰 생성
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .member(member)
                .expiryDate(convertToLocalDateTime(expiryDate))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken findRefreshToken(String refreshToken) {
        return refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh Token을 찾을 수 없습니다."));
    }

    @Override
    @Transactional
    public void deleteRefreshToken(String email) {
        Member member = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        deleteRefreshTokenByUser(member);
    }

    @Override
    @Transactional
    public void deleteRefreshTokenByUser(Member member) {
        refreshTokenRepository.findByMemberId(member.getId())
                .ifPresent(refreshTokenRepository::delete);
    }

    @Override
    public boolean isTokenExpired(RefreshToken token) {
        return token.getExpiryDate().isBefore(LocalDateTime.now());
    }

    private LocalDateTime convertToLocalDateTime(Date date) {
        return Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
