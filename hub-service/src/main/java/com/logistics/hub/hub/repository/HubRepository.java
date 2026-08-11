package com.logistics.hub.hub.repository;

import com.logistics.hub.hub.entity.Hub;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HubRepository extends JpaRepository<Hub, UUID> {

    List<Hub> findByDeletedAtIsNull(); // 목록 조회

    Optional<Hub> findByHubIdAndDeletedAtIsNull(UUID hubId); // 단건 조회

    @Query("SELECT h FROM Hub h WHERE h.deletedAt IS NULL AND (:keyword IS NULL OR h.name LIKE %:keyword%)")
    Page<Hub> search(@Param("keyword") String keyword, Pageable pageable);
}
