package com.logistics.user.user.repository;

import com.logistics.user.user.entity.User;
import com.logistics.user.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, UserRepositoryCustom {
    boolean existsBySlackIdAndDeletedAtIsNullAndStatusNot(String slackId, UserStatus status);

    boolean existsByUsernameAndDeletedAtIsNullAndStatusNot(String username, UserStatus status);

    Optional<User> findByUserIdAndDeletedAtIsNull(UUID userId);
}
