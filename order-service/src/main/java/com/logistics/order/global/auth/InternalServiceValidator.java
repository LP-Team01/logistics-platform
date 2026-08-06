package com.logistics.order.global.auth;

import com.logistics.order.config.InternalServiceProperties;
import com.logistics.order.global.exception.BusinessException;
import com.logistics.order.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
public class InternalServiceValidator {

    private final InternalServiceProperties properties;

    /**
     * Delivery Service 내부 호출 검증
     */
    public void validateDeliveryService(
            String serviceName,
            String serviceKey
    ) {
        boolean validName =
                "delivery-service".equals(serviceName);

        boolean validKey = serviceKey != null
                && MessageDigest.isEqual(
                        properties.key().getBytes(StandardCharsets.UTF_8),
                        serviceKey.getBytes(StandardCharsets.UTF_8)
                );

        if (!validName || !validKey) {
            throw new BusinessException(
                    ErrorCode.ACCESS_DENIED
            );
        }
    }
}
