package com.logistics.company.company.service;

import com.logistics.company.company.domain.Company;
import com.logistics.company.company.dto.CreateRequestDto;
import com.logistics.company.company.dto.ResponseDto;
import com.logistics.company.company.dto.UpdateRequestDto;
import com.logistics.company.company.repository.CompanyRepository;
import com.logistics.company.global.exception.BusinessException;
import com.logistics.company.global.exception.ErrorCode;
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
    public ResponseDto createCompany(CreateRequestDto request, String userId) {

        // 요구사항 반영, 관리 허브 ID가 실제 존재하는지 확인
        validateHubExists(request.hubId());

        Company company = Company.builder()
            .hubId(request.hubId())
            .name(request.name())
            .type(request.type())
            .address(request.address())
            .createdBy(userId)
            .build();

        Company savedCompany = companyRepository.save(company);
        return ResponseDto.from(savedCompany);
    }

    public ResponseDto getCompany(UUID companyId) {
        Company company = companyRepository.findByCompanyIdAndDeletedAtIsNull(companyId)
            .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        return ResponseDto.from(company);
    }

    @Transactional
    public ResponseDto updateCompany(UUID companyId, UpdateRequestDto request, String userId) {
        Company company = companyRepository.findByCompanyIdAndDeletedAtIsNull(companyId)
            .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        // 수정 시 변경하려는 허브 ID가 존재하는지 검증
        validateHubExists(request.hubId());

        company.update(request.hubId(), request.name(), request.type(), request.address(), userId);
        return ResponseDto.from(company);
    }

    @Transactional
    public void deleteCompany(UUID companyId, String userId) {
        Company company = companyRepository.findByCompanyIdAndDeletedAtIsNull(companyId)
            .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        company.delete(userId);
    }

    // 허브 ID 유효성 검증 메서드
    private void validateHubExists(UUID hubId) {
        // TODO : MSA 통신(OpenFeign) 으로 hub-service에 요청을 보내 hubId가 진짜 있는지 검증
    }
}
