package com.logistics.hub.global.auth;

import com.logistics.hub.global.exception.BusinessException;
import com.logistics.hub.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class RoleValidator {

    private static final String MASTER_ROLE = "MASTER";

    public void requireMaster(String role) {
        if (!MASTER_ROLE.equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ROLE);
        }
    }
}
