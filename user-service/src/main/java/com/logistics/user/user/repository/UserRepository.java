package com.logistics.user.user.repository;

import com.logistics.user.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>{
    boolean existsBySlackId(String slackId);

    boolean existsByUsername(String username);
}
