package com.cardtrading.ad.repository;

import com.cardtrading.ad.entity.Ad;
import com.cardtrading.ad.entity.Ad.AdStatus;
import com.cardtrading.ad.entity.Ad.AdType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AdRepository extends JpaRepository<Ad, UUID>, JpaSpecificationExecutor<Ad> {

    Page<Ad> findByUserId(UUID userId, Pageable pageable);

    Page<Ad> findByTypeAndStatus(AdType type, AdStatus status, Pageable pageable);

    Page<Ad> findByStatus(AdStatus status, Pageable pageable);

    Page<Ad> findByCardNameContainingIgnoreCase(String cardName, Pageable pageable);
}
