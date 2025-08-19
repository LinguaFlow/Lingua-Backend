package backend_lingua.linguas.domain.member.entity;

import backend_lingua.linguas.domain.member.enumerated.MemberRole;
import backend_lingua.linguas.domain.oauth.enumerated.MemberRoleConverter;
import backend_lingua.linguas.domain.oauth.enumerated.ProviderType;
import backend_lingua.linguas.domain.oauth.enumerated.ProviderTypeConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false , name = "email")
    private String email;

    @Column(name = "name")
    private String name;

    @Column(name = "picture")
    private String picture;

    @Column(name = "provider")
    @Convert(converter = ProviderTypeConverter.class)
    private ProviderType provider;

    @Column(name = "provider_id")
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Convert(converter = MemberRoleConverter.class)
    private MemberRole role;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
