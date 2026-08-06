package com.logistics.user.user.entity;

import com.logistics.user.global.entity.BaseEntity;
import com.logistics.user.user.dto.request.UpdateRequestDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID userId;

    // 유니크 제약은 활성(soft-delete/REJECTED 아님) 유저에게만 적용되는
    // partial unique index(ux_users_username_active)로 DB에서 관리됨 (V4 마이그레이션)
    @Column(length = 10, nullable = false)
    private String username;

    @Column(length = 100, nullable = false)
    private String password;

    // 유니크 제약은 ux_users_slack_id_active partial unique index로 DB에서 관리됨 (V4 마이그레이션)
    @Column(name = "slack_id", length = 100, nullable = false)
    private String slackId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "hub_id", nullable = true)
    private UUID hubId;

    @Column(name = "company_id", nullable = true)
    private UUID companyId;

    @Builder
    private User(
        String username,
        String encodedPassword,
        String slackId,
        UserRole role,
        UserStatus status,
        UUID hubId,
        UUID companyId
    ) {

        this.username = username;
        this.password = encodedPassword;
        this.slackId = slackId;
        this.role = role;
        this.status = status;
        this.hubId = hubId;
        this.companyId = companyId;
    }



    public void update(UpdateRequestDto requestDto){
        if(requestDto.username() != null){
            this.username = requestDto.username();
        }
        if(requestDto.slackId() != null){
            this.slackId = requestDto.slackId();
        }
        if(requestDto.role() != null){
            this.role = requestDto.role();
        }
        if(requestDto.hubId() != null){
            this.hubId = requestDto.hubId();
        }
        if(requestDto.companyId() != null){
            this.companyId = requestDto.companyId();
        }
    }

    public void changePassword(String password){
        this.password = password;
    }

    public void approve() {
        this.status = UserStatus.APPROVED;
    }

    public void reject() {
        this.status = UserStatus.REJECTED;
    }



}
