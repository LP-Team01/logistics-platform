package com.logistics.company.company.controller;

import com.logistics.company.company.dto.CreateRequestDto;
import com.logistics.company.company.dto.ResponseDto;
import com.logistics.company.company.dto.UpdateRequestDto;
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
    public ResponseEntity<ResponseDto> createCompany(
        @Valid @RequestBody CreateRequestDto request,
        @RequestHeader(value = "X-User-Id", required = false, defaultValue = "anonymousUser") String userId
    ) {
        ResponseDto response = companyService.createCompany(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<ResponseDto> getCompany(@PathVariable("companyId") UUID companyId) {
        ResponseDto response = companyService.getCompany(companyId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{companyId}")
    public ResponseEntity<ResponseDto> updateCompany(
        @PathVariable("companyId") UUID companyId,
        @Valid @RequestBody UpdateRequestDto request,
        @RequestHeader(value = "X-User-Id", required = false, defaultValue = "anonymousUser") String userId
    ) {
        ResponseDto response = companyService.updateCompany(companyId, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{companyId}")
    public ResponseEntity<Void> deleteCompany(
        @PathVariable("companyId") UUID companyId,
        @RequestHeader(value = "X-User-Id", required = false, defaultValue = "anonymousUser") String userId
    ) {
        companyService.deleteCompany(companyId, userId);
        return ResponseEntity.noContent().build();
    }
}
