package backend_lingua.linguas.member.repository;

import backend_lingua.linguas.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmailAndProvider(String email, String provider);
    Optional<Member> findByEmail(String email);
}

