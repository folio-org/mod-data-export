package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;


public interface ExportIdEntityRepository extends JpaRepository<ExportIdEntity, Integer> {

  long countByJobExecutionId(UUID jobExecutionId);

  List<ExportIdEntity> findByInstanceIdInAndJobExecutionIdIs(Set<UUID> ids, UUID jobExecutionId);

  Slice<ExportIdEntity> findByJobExecutionIdIsAndInstanceIdGreaterThanEqualAndInstanceIdLessThanEqualOrderByInstanceIdAsc(UUID jobExecutionId, UUID fromId, UUID toId, Pageable page);

  long countByJobExecutionIdIsAndInstanceIdGreaterThanEqualAndInstanceIdLessThanEqual(UUID jobExecutionId, UUID fromId, UUID toId);

  @Modifying
  @Query("DELETE ExportIdEntity e WHERE e.jobExecutionId = :jobExecutionId")
  int deleteWithJobExecutionId(@Param("jobExecutionId") UUID jobExecutionId);

  default Slice<ExportIdEntity> getExportIds(UUID jobExecutionId, UUID fromId, UUID toId, Pageable page) {
    return findByJobExecutionIdIsAndInstanceIdGreaterThanEqualAndInstanceIdLessThanEqualOrderByInstanceIdAsc(jobExecutionId, fromId, toId, page);
  }

  default long countExportIds(UUID jobExecutionId, UUID fromId, UUID toId) {
    return countByJobExecutionIdIsAndInstanceIdGreaterThanEqualAndInstanceIdLessThanEqual(jobExecutionId, fromId, toId);
  }
}
