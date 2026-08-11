package com.logistics.order.infrastructure.client;

import com.logistics.order.infrastructure.client.dto.CompanyResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Company Service 업체 조회
 */
@FeignClient(
        name = "company-service",
        path = "/api/companies"
)
public interface CompanyClient {

    /**
     * 업체 ID로 담당 허브와 주소 조회
     */
    @GetMapping("/{companyId}")
    CompanyResponse getCompany(
            @PathVariable UUID companyId
    );
}
