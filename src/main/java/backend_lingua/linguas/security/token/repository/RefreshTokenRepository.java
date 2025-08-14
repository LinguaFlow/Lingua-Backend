package backend_lingua.linguas.security.token.repository;


import backend_lingua.linguas.security.token.dto.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 토큰으로 조회 (member 함께 조회 - N+1 문제 해결)
     */
    @Query("SELECT rt FROM RefreshToken rt JOIN FETCH rt.member WHERE rt.token = :token")
    Optional<RefreshToken> findByTokenWithMember(@Param("token") String token);

    /**
     * 일반 토큰 조회
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 사용자 ID로 토큰 조회
     */
    Optional<RefreshToken> findByMemberId(Long memberId);

    /**
     * 사용자 ID로 토큰 삭제 (벌크 연산)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.member.id = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);

    /**
     * 사용자 이메일로 토큰 삭제 (벌크 연산)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.member.email = :email")
    void deleteByMemberEmail(@Param("email") String email);

    /**
     * 만료된 토큰 삭제 (스케줄러용)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * 사용자별 토큰 개수 조회
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.member.id = :memberId")
    long countByMemberId(@Param("memberId") Long memberId);
}
