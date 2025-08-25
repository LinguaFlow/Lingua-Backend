package backend_lingua.linguas.infrastructure.security.token.service;

import backend_lingua.linguas.domain.member.entity.Member;
import backend_lingua.linguas.domain.member.repository.MemberRepository;
import backend_lingua.linguas.infrastructure.security.token.entity.RefreshToken;
import backend_lingua.linguas.infrastructure.security.token.repository.RefreshTokenRepository;
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
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(String token, Date expiryDate, Authentication authentication) {
        String email = authentication.getName();

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));
        // 새 토큰 생성
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .member(member)
                .expiryDate(convertToLocalDateTime(expiryDate))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public void deleteRefreshToken(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        deleteRefreshTokenByUser(member);
    }

    @Override
    @Transactional
    public void deleteRefreshTokenByUser(Member member) {
        refreshTokenRepository.findByMemberId(member.getId())
                .ifPresent(refreshTokenRepository::delete);
    }

    private LocalDateTime convertToLocalDateTime(Date date) {
        return Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
