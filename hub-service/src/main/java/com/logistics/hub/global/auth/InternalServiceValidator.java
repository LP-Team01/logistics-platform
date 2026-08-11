package com.logistics.hub.global.auth;

import com.logistics.hub.global.config.InternalServiceProperties;
import com.logistics.hub.global.exception.BusinessException;
import com.logistics.hub.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
public class InternalServiceValidator {

    private final InternalServiceProperties properties;

    public void validate(String serviceName, String serviceKey) {

        boolean validName = properties.allowedName().equals(serviceName);

        boolean validKey = serviceKey != null
            && MessageDigest.isEqual(
                properties.key().getBytes(StandardCharsets.UTF_8),
            serviceKey.getBytes(StandardCharsets.UTF_8)
        );

        if (!validName || !validKey) {
            throw new BusinessException(ErrorCode.INTERNAL_ACCESS_DENIED);
        }
    }
}
