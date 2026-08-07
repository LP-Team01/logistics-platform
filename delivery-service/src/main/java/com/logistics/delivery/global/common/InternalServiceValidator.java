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

    // Order Service 내부 호출 검증
    public void validateOrderService(String serviceName, String serviceKey) {
        boolean validName = "order-service".equals(serviceName);

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