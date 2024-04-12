package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.AuditInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AuditInstanceEntityRepository extends JpaRepository<AuditInstanceEntity, UUID> {
  List<AuditInstanceEntity> findByIdIn(Set<UUID> ids);
}
