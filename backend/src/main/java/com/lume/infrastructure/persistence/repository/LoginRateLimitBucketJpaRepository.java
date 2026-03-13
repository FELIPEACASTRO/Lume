package com.lume.infrastructure.persistence.repository;

import com.lume.infrastructure.persistence.entity.LoginRateLimitBucketJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface LoginRateLimitBucketJpaRepository extends JpaRepository<LoginRateLimitBucketJpaEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select bucket from LoginRateLimitBucketJpaEntity bucket where bucket.bucketKey = :bucketKey")
    Optional<LoginRateLimitBucketJpaEntity> findLockedByBucketKey(@Param("bucketKey") String bucketKey);

    @Modifying
    @Query("delete from LoginRateLimitBucketJpaEntity bucket where bucket.updatedAt < :cutoff")
    void deleteByUpdatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
