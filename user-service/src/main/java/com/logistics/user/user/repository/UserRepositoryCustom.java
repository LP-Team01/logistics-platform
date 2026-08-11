package com.logistics.user.user.repository;

import com.logistics.user.user.dto.request.UserSearchCondition;
import com.logistics.user.user.entity.User;
import com.logistics.user.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserRepositoryCustom {
    Page<User> search(UserSearchCondition condition, Pageable pageable);

    Page<User> findAllByHubIdAndDeletedAtIsNullAndStatus(UUID hubId, UserStatus status, Pageable pageable);
}
