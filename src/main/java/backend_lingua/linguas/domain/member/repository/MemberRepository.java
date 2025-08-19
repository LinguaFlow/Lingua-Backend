package backend_lingua.linguas.domain.member.repository;

import backend_lingua.linguas.domain.member.entity.Member;
import backend_lingua.linguas.domain.oauth.enumerated.ProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("SELECT m FROM Member m WHERE m.email = :email AND m.provider = :provider")
    Optional<Member> findByEmailAndProvider(@Param("email") String email,
                                            @Param("provider") ProviderType provider);  // ✅ 타입 변경

    Optional<Member> findByEmail(String email);
}

