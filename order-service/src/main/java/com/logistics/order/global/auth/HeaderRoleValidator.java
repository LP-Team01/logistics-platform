package com.logistics.order.global.auth;

import com.logistics.order.global.exception.BusinessException;
import com.logistics.order.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class HeaderRoleValidator {
    public void validate(
            String userRole,
            String... allowedRoles
    ) {
        boolean allowed = userRole != null
                && Arrays.asList(allowedRoles).contains(userRole);

        if (!allowed) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
