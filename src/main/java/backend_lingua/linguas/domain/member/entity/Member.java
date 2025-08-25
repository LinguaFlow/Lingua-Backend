package backend_lingua.linguas.domain.member.entity;

import backend_lingua.linguas.domain.member.enumerated.MemberRole;
import backend_lingua.linguas.domain.oauth.enumerated.MemberRoleConverter;
import backend_lingua.linguas.domain.oauth.enumerated.ProviderType;
import backend_lingua.linguas.domain.oauth.enumerated.ProviderTypeConverter;
import backend_lingua.linguas.global.config.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@Table(name = "users")
@Getter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Member extends BaseEntity {

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
    @Column(name = "role")
    @Convert(converter = MemberRoleConverter.class)
    private MemberRole role;

}
