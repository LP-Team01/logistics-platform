package com.logistics.user.user.repository;

import com.logistics.user.user.dto.request.UserSearchCondition;
import com.logistics.user.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepositoryCustom {
    Page<User> search(UserSearchCondition condition, Pageable pageable);
}
