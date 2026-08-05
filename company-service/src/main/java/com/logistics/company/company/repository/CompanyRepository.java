package com.logistics.company.company.repository;

import com.logistics.company.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    // 삭제되지 않은 (deletedAt이 null인) 업체만 조회하는 Spring Data JPA 메서드
    Optional<Company> findByCompanyIdAndDeletedAtIsNull(UUID companyId);
}
