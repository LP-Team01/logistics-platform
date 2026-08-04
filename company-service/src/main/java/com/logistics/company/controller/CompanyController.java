package com.logistics.company.controller;

import com.logistics.company.dto.CompanyCreateRequest;
import com.logistics.company.dto.CompanyResponse;
import com.logistics.company.dto.CompanyUpdateRequest;
import com.logistics.company.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    // 업체 생성 API , 권한 : MASTER, HUB_MANAGER (담당 허브)
    @PostMapping
    public ResponseEntity<CompanyResponse> createCompany(
        @Valid @RequestBody CompanyCreateRequest request,
        @RequestHeader(value = "X-User-Passport", required = false, defaultValue = "anonymousUser") String passportUserHeader
        ) {
        CompanyResponse response = companyService.createCompany(request, passportUserHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 업체 단건 조회 API , 권한 : 전원 접근 가능
    @GetMapping("/{companyId}")
    public ResponseEntity<CompanyResponse> getCompany(@PathVariable("companyId") UUID companyId) {
        CompanyResponse response = companyService.getCompany(companyId);
        return ResponseEntity.ok(response);
    }

    // 업체 수정 API , 권한 : MASTER, HUB_MANAGER(담당 허브), COMPANY_MANAGER(본인 업체)
    @PutMapping("/{companyId}")
    public ResponseEntity<CompanyResponse> updateCompany(
        @PathVariable("companyId") UUID companyId,
        @Valid @RequestBody CompanyUpdateRequest request,
        @RequestHeader(value = "X-User-Passport", required = false, defaultValue = "anonymousUser") String passportUserHeader
    ) {
        CompanyResponse response = companyService.updateCompany(companyId, request, passportUserHeader);
        return ResponseEntity.ok(response);
    }

    // 업체 삭제 API , 권한 : MASTER, HUB_MANAGER(담당 허브)
    @DeleteMapping("/{companyId}")
    public ResponseEntity<Void> deleteCompany(
        @PathVariable("companyId") UUID companyId,
        @RequestHeader(value = "X-User-Passport", required = false, defaultValue = "anonymousUser") String passportUserHeader
    ) {
        companyService.deleteCompany(companyId, passportUserHeader);
        return ResponseEntity.noContent().build();
    }

}
