package org.folio.dataexp.repository;

import java.util.List;
import java.util.UUID;
import org.folio.dataexp.domain.entity.ErrorLogEntity;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

/** Repository for {@link ErrorLogEntity}. */
public interface ErrorLogEntityCqlRepository extends JpaCqlRepository<ErrorLogEntity, UUID> {

  /**
   * Gets error logs by job execution ID and error codes string.
   *
   * @param jobExecutionId job execution UUID
   * @param errorCodesString error codes string
   * @return list of error log entities
   */
  @Query(
      value =
          "SELECT * FROM error_logs WHERE cast(jsonb ->> 'jobExecutionId' AS uuid) = ?1"
              + " AND jsonb ->> 'errorMessageCode' SIMILAR TO ?2",
      nativeQuery = true)
  List<ErrorLogEntity> getByJobExecutionIdAndErrorCodes(
      UUID jobExecutionId, String errorCodesString);

  /**
   * Inserts an error log if it does not already exist.
   *
   * @param id error log UUID
   * @param jsonb error log as jsonb string
   * @param creationDate creation date
   * @param createdBy creator
   * @param jobExecutionId job execution UUID
   */
  @Modifying
  @Transactional
  @Query(
      value =
          "INSERT INTO error_logs (id, jsonb, creation_date, created_by,"
              + "job_execution_id, jobprofileid)"
              + " VALUES (?, ?::jsonb, ?, ?, ?, ?)"
              + " ON CONFLICT DO NOTHING",
      nativeQuery = true)
  void insertIfNotExists(
      UUID id,
      String jsonb,
      java.util.Date creationDate,
      String createdBy,
      UUID jobExecutionId,
      UUID jobProfileId);

  /**
   * Gets error logs by job execution ID and error code.
   *
   * @param jobExecutionId job execution UUID
   * @param errorCode error code
   * @return list of error log entities
   */
  @Query(
      value =
          "SELECT * FROM error_logs WHERE cast(jsonb ->> 'jobExecutionId' AS uuid) = ?1"
              + " AND jsonb ->> 'errorMessageCode' = ?2",
      nativeQuery = true)
  List<ErrorLogEntity> getByJobExecutionIdAndErrorCode(UUID jobExecutionId, String errorCode);

  /**
   * Gets all error logs by job execution ID.
   *
   * @param jobExecutionId job execution UUID
   * @return list of error log entities
   */
  @Query(
      value = "SELECT * FROM error_logs WHERE cast(jsonb ->> 'jobExecutionId' AS uuid) = ?1",
      nativeQuery = true)
  List<ErrorLogEntity> getAllByJobExecutionId(UUID jobExecutionId);

  /**
   * Counts error logs by job execution ID.
   *
   * @param jobExecutionId job execution UUID
   * @return count of error logs
   */
  long countByJobExecutionId(UUID jobExecutionId);

  /**
   * Deletes error logs by job profile ID.
   *
   * @param jobProfileId job profile UUID
   * @return number of deleted error logs
   */
  long deleteByJobProfileId(UUID jobProfileId);
}
