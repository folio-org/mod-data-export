package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.ErrorLogEntity;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ErrorLogEntityCqlRepository extends JpaCqlRepository<ErrorLogEntity, UUID> {
  @Query(value = "SELECT * FROM error_logs WHERE cast(jsonb ->> 'jobExecutionId' AS uuid) = ?1" +
    " AND jsonb ->> 'errorMessageCode' SIMILAR TO ?2", nativeQuery = true)
  List<ErrorLogEntity> getByJobExecutionIdAndErrorCodes(UUID jobExecutionId, String errorCodesString);

  @Query(value = "SELECT * FROM error_logs WHERE cast(jsonb ->> 'jobExecutionId' AS uuid) = ?1" +
    " AND jsonb ->> 'errorMessageCode' = ?2", nativeQuery = true)
  List<ErrorLogEntity> getByJobExecutionIdAndErrorCode(UUID jobExecutionId, String errorCode);
}
