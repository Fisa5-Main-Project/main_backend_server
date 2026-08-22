package com.know_who_how.main_server.mydata.repository;

import com.know_who_how.main_server.mydata.entity.MydataSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface MydataSnapshotRepository extends JpaRepository<MydataSnapshot, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO mydata_snapshot (
                user_id, payload, schema_version, source_fetched_at,
                synced_at, created_at, updated_at
            ) VALUES (
                :userId, :payload, :schemaVersion, :sourceFetchedAt,
                :syncedAt, :syncedAt, :syncedAt
            )
            ON DUPLICATE KEY UPDATE
                payload = IF(VALUES(source_fetched_at) >= source_fetched_at, VALUES(payload), payload),
                schema_version = IF(VALUES(source_fetched_at) >= source_fetched_at, VALUES(schema_version), schema_version),
                synced_at = IF(VALUES(source_fetched_at) >= source_fetched_at, VALUES(synced_at), synced_at),
                updated_at = IF(VALUES(source_fetched_at) >= source_fetched_at, VALUES(updated_at), updated_at),
                source_fetched_at = GREATEST(source_fetched_at, VALUES(source_fetched_at))
            """, nativeQuery = true)
    int upsert(
            @Param("userId") Long userId,
            @Param("payload") String payload,
            @Param("schemaVersion") int schemaVersion,
            @Param("sourceFetchedAt") LocalDateTime sourceFetchedAt,
            @Param("syncedAt") LocalDateTime syncedAt
    );
}
