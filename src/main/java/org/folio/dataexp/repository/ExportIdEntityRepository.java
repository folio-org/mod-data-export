package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface ExportIdEntityRepository extends JpaRepository<ExportIdEntity, Integer> {

  long countByJobExecutionId(UUID jobExecutionId);

  Slice<ExportIdEntity> findByJobExecutionIdIsAndInstanceIdGreaterThanEqualAndInstanceIdLessThanEqualOrderByInstanceIdAsc(UUID jobExecutionId, UUID fromId, UUID toId, Pageable page);

  long countByJobExecutionIdIsAndInstanceIdGreaterThanEqualAndInstanceIdLessThanEqual(UUID jobExecutionId, UUID fromId, UUID toId);

  default Slice<ExportIdEntity> getExportIds(UUID jobExecutionId, UUID fromId, UUID toId, Pageable page) {
    return findByJobExecutionIdIsAndInstanceIdGreaterThanEqualAndInstanceIdLessThanEqualOrderByInstanceIdAsc(jobExecutionId, fromId, toId, page);
  }

  default long countExportIds(UUID jobExecutionId, UUID fromId, UUID toId) {
    return countByJobExecutionIdIsAndInstanceIdGreaterThanEqualAndInstanceIdLessThanEqual(jobExecutionId, fromId, toId);
  }
}