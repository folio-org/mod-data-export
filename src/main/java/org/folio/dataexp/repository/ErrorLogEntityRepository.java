package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.ErrorLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ErrorLogEntityRepository extends JpaRepository<ErrorLogEntity, UUID> {
  @Query(value = "SELECT * FROM error_logs where jsonb ->> 'jobExecutionId' = ?1" +
    " and el.jsonb ->> 'errorMessageCode' SIMILAR TO ?2", nativeQuery = true)
  List<ErrorLogEntity> getByJobExecutionIdAndErrorCodes(UUID jobExecutionId, String errorCodesString);

  @Query(value = "SELECT * FROM error_logs where jsonb ->> 'jobExecutionId' = ?1" +
    " and el.jsonb ->> 'errorMessageCode' = ?2", nativeQuery = true)
  List<ErrorLogEntity> getByJobExecutionIdAndErrorCode(UUID jobExecutionId, String errorCode);
}
