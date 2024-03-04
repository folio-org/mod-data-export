package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface HoldingsRecordEntityRepository extends JpaRepository<HoldingsRecordEntity, UUID> {

  List<HoldingsRecordEntity> findByIdIn(Set<UUID> ids);

  List<HoldingsRecordEntity> findByInstanceIdIs(UUID instanceId);
}
