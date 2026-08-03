package com.logistics.user.dto.response;

import com.logistics.user.core.User;
import com.logistics.user.core.UserRole;
import com.logistics.user.core.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UserResponseDto {
    private UUID userId;
    private String username;
    private UserRole role;
    private UserStatus status;
    private UUID hubId;
    private UUID companyId;


    public static UserResponseDto from(User user){
        return UserResponseDto.builder()
            .userId(user.getUserId())
            .username(user.getUsername())
            .role(user.getRole())
            .status(user.getStatus())
            .hubId(user.getHubId())
            .companyId(user.getCompanyId())
            .build();
    }
}
