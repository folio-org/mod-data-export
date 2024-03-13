package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface FileDefinitionEntityRepository extends JpaRepository<FileDefinitionEntity, UUID> {
  @Query(value = "SELECT * FROM file_definitions WHERE jsonb ->> 'sourcePath' IS NOT NULL" +
    " AND to_timestamp(cast(jsonb -> 'metadata' ->> 'updatedDate' AS BIGINT) / 1000) <= ?1", nativeQuery = true)
  List<FileDefinitionEntity> getExpiredEntities(Date expirationDate);

  @Query(value = "SELECT * FROM file_definitions WHERE jsonb ->> 'jobExecutionId' = ?1", nativeQuery = true)
  List<FileDefinitionEntity> getFileDefinitionByJobExecutionId(String jobExecutionId);
}
