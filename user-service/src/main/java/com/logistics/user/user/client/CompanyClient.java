package com.logistics.user.user.client;

import com.logistics.user.global.config.FeignRetryConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "company-service", configuration = FeignRetryConfig.class)
public interface CompanyClient {
    @GetMapping("/api/companies/{companyId}")
    CompanyResponseDto getCompany(@PathVariable UUID companyId);
}
