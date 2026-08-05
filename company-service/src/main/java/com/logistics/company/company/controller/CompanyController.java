package com.logistics.company.company.controller;

import com.logistics.company.company.dto.CompanyCreateRequestDto;
import com.logistics.company.company.dto.CompanyResponseDto;
import com.logistics.company.company.dto.CompanyUpdateRequestDto;
import com.logistics.company.company.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<CompanyResponseDto> createCompany(
        @Valid @RequestBody CompanyCreateRequestDto request,
        @RequestHeader(value = "X-User-Passport", required = false, defaultValue = "anonymousUser") String passportUserHeader
    ) {
        CompanyResponseDto response = companyService.createCompany(request, passportUserHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<CompanyResponseDto> getCompany(@PathVariable("companyId") UUID companyId) {
        CompanyResponseDto response = companyService.getCompany(companyId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{companyId}")
    public ResponseEntity<CompanyResponseDto> updateCompany(
        @PathVariable("companyId") UUID companyId,
        @Valid @RequestBody CompanyUpdateRequestDto request,
        @RequestHeader(value = "X-User-Passport", required = false, defaultValue = "anonymousUser") String passportUserHeader
    ) {
        CompanyResponseDto response = companyService.updateCompany(companyId, request, passportUserHeader);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{companyId}")
    public ResponseEntity<Void> deleteCompany(
        @PathVariable("companyId") UUID companyId,
        @RequestHeader(value = "X-User-Passport", required = false, defaultValue = "anonymousUser") String passportUserHeader
    ) {
        companyService.deleteCompany(companyId, passportUserHeader);
        return ResponseEntity.noContent().build();
    }
}
