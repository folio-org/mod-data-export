package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface MarcRecordEntityRepository extends Repository<MarcRecordEntity, UUID> {

  List<MarcRecordEntity> findByExternalIdInAndRecordTypeIsAndStateIsAndLeaderRecordStatusNot(Set<UUID> ids, String recordType, String state, Character leaderRecordStatus);

  // To find non-deleted
  List<MarcRecordEntity> findByExternalIdInAndRecordTypeIsAndStateIsAndLeaderRecordStatusNotAndSuppressDiscoveryIs(Set<UUID> ids,
      String recordType, String state, Character leaderRecordStatus, Boolean suppressDiscovery);

  // To find all including deleted
  List<MarcRecordEntity> findByExternalIdInAndRecordTypeIsAndSuppressDiscoveryIs(Set<UUID> ids, String recordType, Boolean suppressDiscovery);
}
