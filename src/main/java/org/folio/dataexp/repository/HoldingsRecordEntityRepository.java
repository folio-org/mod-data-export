package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface HoldingsRecordEntityRepository extends Repository<HoldingsRecordEntity, UUID> {

  List<HoldingsRecordEntity> findByIdIn(Set<UUID> ids);

  List<HoldingsRecordEntity> findByInstanceIdIs(UUID instanceId);
}
