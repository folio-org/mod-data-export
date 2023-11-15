package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;


public interface ExportIdEntityRepository extends JpaRepository<ExportIdEntity, Integer> {

  long countByJobExecutionId(UUID jobExecutionId);

  List<ExportIdEntity> findByJobExecutionIdIsAndInstanceIdGreaterThanEqualAndInstanceIdLessThanEqualOrderByInstanceIdAsc(UUID jobExecutionId, UUID fromId, UUID toId);
}
