package com.logistics.hub.domain.repository;

import com.logistics.hub.domain.entity.Hub;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HubRepository extends JpaRepository<Hub, UUID> {

    List<Hub> findByDeletedAtIsNull(); // 목록 조회

    Optional<Hub> findByHubIdAndDeletedAtIsNull(UUID hubId); // 단건 조회
}
