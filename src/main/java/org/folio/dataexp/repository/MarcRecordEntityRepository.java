package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface MarcRecordEntityRepository extends JpaRepository<MarcRecordEntity, UUID> {

  List<MarcRecordEntity> findByExternalIdIn(Set<UUID> ids);
}
