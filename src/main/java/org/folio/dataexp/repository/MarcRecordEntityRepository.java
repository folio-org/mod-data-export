package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface MarcRecordEntityRepository extends Repository<MarcRecordEntity, UUID> {

  List<MarcRecordEntity> findByExternalIdInAndRecordTypeIsAndStateIn(Set<UUID> ids, String recordType, Set<String> states);
}
