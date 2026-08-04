package com.logistics.user.core;

import com.logistics.user.dto.request.UpdateRequestDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "p_users")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
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


    public static User signup(
        String username,
        String encodedPassword,
        String slackId,
        UserRole role,
        UUID hubId,
        UUID companyId
    ) {

        return User.builder()
            .username(username)
            .password(encodedPassword)
            .slackId(slackId)
            .role(role)
            .status(UserStatus.PENDING)
            .hubId(hubId)
            .companyId(companyId)
            .build();
    }

    public static User createByMaster(
        String username,
        String encodedPassword,
        String slackId,
        UserRole role,
        UUID hubId,
        UUID companyId
    ) {

        return User.builder()
            .username(username)
            .password(encodedPassword)
            .slackId(slackId)
            .role(role)
            .status(UserStatus.APPROVED)
            .hubId(hubId)
            .companyId(companyId)
            .build();
    }

    public void update(UpdateRequestDto requestDto){
        if(requestDto.getUsername() != null){
            this.username = requestDto.getUsername();
        }
        if(requestDto.getSlackId() != null){
            this.slackId = requestDto.getSlackId();
        }
        if(requestDto.getRole() != null){
            this.role = requestDto.getRole();
        }
        if(requestDto.getHubId() != null){
            this.hubId = requestDto.getHubId();
        }
        if(requestDto.getCompanyId() != null){
            this.companyId = requestDto.getCompanyId();
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
