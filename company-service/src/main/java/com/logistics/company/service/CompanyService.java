package com.logistics.company.service;


import com.logistics.company.domain.Company;
import com.logistics.company.dto.CompanyCreateRequestDto;
import com.logistics.company.dto.CompanyResponseDto;
import com.logistics.company.dto.CompanyUpdateRequestDto;
import com.logistics.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    // TODO: 추후 client-hub 오픈 페인(FeignClient) 추가 위치

    @Transactional
    public CompanyResponseDto createCompany(CompanyCreateRequestDto request, String passportUserHeader) {

        // 요구사항 반영, 관리 허브 ID가 실제 존재하는지 확인
        validateHubExists(request.getHubId());

        Company company = Company.builder()
            .hubId(request.getHubId())
            .name(request.getName())
            .type(request.getType())
            .address(request.getAddress())
            .createdBy(passportUserHeader) // 헤더나 토큰에서 추출한 사용자
            .build();

        Company savedCompany = companyRepository.save(company);
        return CompanyResponseDto.from(savedCompany);
    }


    public CompanyResponseDto getCompany(UUID companyId) {
        Company company = companyRepository.findByCompanyIdAndDeletedAtIsNull(companyId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 삭제된 업체입니다. ID: " + companyId));

        return CompanyResponseDto.from(company);
    }

    @Transactional
    public CompanyResponseDto updateCompany(UUID companyId, CompanyUpdateRequestDto request, String passportUserHeader) {
        Company company = companyRepository.findByCompanyIdAndDeletedAtIsNull(companyId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 삭제된 업체입니다. ID: " + companyId));

        // 수정 시 변경하려는 허브 ID가 존재하는지 검증
        validateHubExists(request.getHubId());

        company.update(request.getHubId(), request.getName(), request.getType(), request.getAddress(), passportUserHeader);
        return CompanyResponseDto.from(company);
    }

    @Transactional
    public void deleteCompany(UUID companyId, String passportUserHeader) {
        Company company = companyRepository.findByCompanyIdAndDeletedAtIsNull(companyId)
            .orElseThrow(()-> new IllegalArgumentException("존재하지 않거나 이미 삭제된 업체입니다. ID: " + companyId));

        company.delete(passportUserHeader);
    }

    // 허브 ID 유효성 검증 메서드
    private void validateHubExists(UUID hubId) {
        // TODO : MSA 통신(OpenFeign) 으로 hub-service에 요청을 보내 hubId가 진짜 있는지 검증
    }
}
