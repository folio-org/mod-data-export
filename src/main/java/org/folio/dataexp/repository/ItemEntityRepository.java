package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.ItemEntity;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface ItemEntityRepository extends Repository<ItemEntity, UUID> {

  List<ItemEntity> findByHoldingsRecordIdIs(UUID holdingsRecordId);
}
