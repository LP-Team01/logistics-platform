package com.logistics.delivery.infrastructure.client;

import com.logistics.delivery.infrastructure.client.dto.CompanyServiceCompanyResponseDto;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "company-service")
public interface CompanyServiceClient {

    @GetMapping("/api/companies/{companyId}")
    CompanyServiceCompanyResponseDto getCompany(@PathVariable("companyId") UUID companyId);
}