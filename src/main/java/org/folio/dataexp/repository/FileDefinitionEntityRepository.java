package org.folio.dataexp.repository;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Repository for {@link FileDefinitionEntity}.
 */
public interface FileDefinitionEntityRepository extends JpaRepository<FileDefinitionEntity, UUID> {

  /**
   * Gets expired file definition entities.
   *
   * @param expirationDate expiration date
   * @return list of expired file definition entities
   */
  @Query(
      value = "SELECT * FROM file_definitions WHERE jsonb ->> 'sourcePath' IS NOT NULL"
      + " AND to_timestamp(cast(jsonb -> 'metadata' ->> 'updatedDate' AS BIGINT) / 1000) <= ?1",
      nativeQuery = true
  )
  List<FileDefinitionEntity> getExpiredEntities(Date expirationDate);

  /**
   * Gets file definitions by job execution ID.
   *
   * @param jobExecutionId job execution ID
   * @return list of file definition entities
   */
  @Query(
      value = "SELECT * FROM file_definitions WHERE jsonb ->> 'jobExecutionId' = ?1",
      nativeQuery = true
  )
  List<FileDefinitionEntity> getFileDefinitionByJobExecutionId(String jobExecutionId);
}
