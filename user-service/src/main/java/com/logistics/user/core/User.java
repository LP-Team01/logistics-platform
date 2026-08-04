package com.logistics.user.core;

import com.logistics.user.dto.request.UpdateRequestDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "p_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID userId;

    @Column(length = 10, nullable = false, unique = true)
    private String username;

    @Column(length = 100, nullable = false)
    private String password;

    @Column(name = "slack_id", length = 100, nullable = false, unique = true)
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
