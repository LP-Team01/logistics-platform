package com.logistics.ai.global.common;

import com.logistics.ai.global.config.InternalServiceProperties;
import com.logistics.ai.global.exception.BusinessException;
import com.logistics.ai.global.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InternalServiceValidator {

    private final InternalServiceProperties properties;

    // 내부 서비스(delivery-service 등) 호출 검증 - allowedNames에 등록된 서비스만 허용
    public void validateInternalService(String serviceName, String serviceKey) {
        boolean validName = serviceName != null && properties.allowedNames().contains(serviceName);

        boolean validKey = serviceKey != null
                && MessageDigest.isEqual(
                        properties.key().getBytes(StandardCharsets.UTF_8),
                        serviceKey.getBytes(StandardCharsets.UTF_8)
                );

        if (!validName || !validKey) {
            throw new BusinessException(ErrorCode.AI_INTERNAL_FORBIDDEN);
        }
    }
}