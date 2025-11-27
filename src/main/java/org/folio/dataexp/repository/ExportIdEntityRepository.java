package org.folio.dataexp.repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository for {@link ExportIdEntity}. */
public interface ExportIdEntityRepository extends JpaRepository<ExportIdEntity, Integer> {

  /**
   * Counts export IDs by job execution ID.
   *
   * @param jobExecutionId job execution UUID
   * @return count of export IDs
   */
  long countByJobExecutionId(UUID jobExecutionId);

  /**
   * Finds export IDs by instance IDs and job execution ID.
   *
   * @param ids set of instance UUIDs
   * @param jobExecutionId job execution UUID
   * @return list of export ID entities
   */
  List<ExportIdEntity> findByInstanceIdInAndJobExecutionIdIs(Set<UUID> ids, UUID jobExecutionId);

  /**
   * Finds export IDs by job execution ID and instance ID range.
   *
   * @param jobExecutionId job execution UUID
   * @param fromId start instance UUID
   * @param toId end instance UUID
   * @param page pageable
   * @return slice of export ID entities
   */
  Slice<ExportIdEntity>
      findByJobExecutionIdIsAndInstanceIdGreaterThanEqualAndInstanceIdLessThanEqualOrderByInstanceIdAsc(
          UUID jobExecutionId, UUID fromId, UUID toId, Pageable page);

  /**
   * Counts export IDs by job execution ID and instance ID range.
   *
   * @param jobExecutionId job execution UUID
   * @param fromId start instance UUID
   * @param toId end instance UUID
   * @return count of export IDs
   */
  long countByJobExecutionIdIsAndInstanceIdGreaterThanEqualAndInstanceIdLessThanEqual(
      UUID jobExecutionId, UUID fromId, UUID toId);

  /**
   * Deletes export IDs with job execution ID.
   *
   * @param jobExecutionId job execution UUID
   * @return number of deleted rows
   */
  @Modifying
  @Query("DELETE ExportIdEntity e WHERE e.jobExecutionId = :jobExecutionId")
  int deleteWithJobExecutionId(@Param("jobExecutionId") UUID jobExecutionId);

  /**
   * Inserts export ID.
   *
   * @param jobExecutionId job execution UUID
   * @param instanceId instance UUID
   */
  @Modifying
  @Query(
      value =
          "INSERT INTO job_executions_export_ids (job_execution_id, instance_id)"
              + " VALUES (?1, ?2)"
              + " ON CONFLICT DO NOTHING",
      nativeQuery = true)
  void insertExportId(UUID jobExecutionId, UUID instanceId);

  /**
   * Gets export IDs by job execution ID and instance ID range.
   *
   * @param jobExecutionId job execution UUID
   * @param fromId start instance UUID
   * @param toId end instance UUID
   * @param page pageable
   * @return slice of export ID entities
   */
  default Slice<ExportIdEntity> getExportIds(
      UUID jobExecutionId, UUID fromId, UUID toId, Pageable page) {
    return findByJobExecutionIdIsAndInstanceIdGreaterThanEqualAndInstanceIdLessThanEqualOrderByInstanceIdAsc(
        jobExecutionId, fromId, toId, page);
  }

  /**
   * Counts export IDs by job execution ID and instance ID range.
   *
   * @param jobExecutionId job execution UUID
   * @param fromId start instance UUID
   * @param toId end instance UUID
   * @return count of export IDs
   */
  default long countExportIds(UUID jobExecutionId, UUID fromId, UUID toId) {
    return countByJobExecutionIdIsAndInstanceIdGreaterThanEqualAndInstanceIdLessThanEqual(
        jobExecutionId, fromId, toId);
  }
}
