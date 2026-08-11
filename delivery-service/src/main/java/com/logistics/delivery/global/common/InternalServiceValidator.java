package com.logistics.delivery.global.common;

import com.logistics.delivery.global.config.InternalServiceProperties;
import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InternalServiceValidator {

    private final InternalServiceProperties properties;

    // 내부 서비스(order-service, ai-notification-service 등) 호출 검증 - allowedNames에 등록된 서비스만 허용
    public void validateInternalService(String serviceName, String serviceKey) {
        boolean validName = serviceName != null && properties.allowedNames().contains(serviceName);

        boolean validKey = serviceKey != null
                && MessageDigest.isEqual(
                        properties.key().getBytes(StandardCharsets.UTF_8),
                        serviceKey.getBytes(StandardCharsets.UTF_8)
                );

        if (!validName || !validKey) {
            throw new BusinessException(ErrorCode.DELIVERY_INTERNAL_FORBIDDEN);
        }
    }
}