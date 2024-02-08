package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface MarcRecordEntityRepository extends Repository<MarcRecordEntity, UUID> {

  List<MarcRecordEntity> findByExternalIdInAndRecordTypeIs(Set<UUID> ids, String recordType);
  List<MarcRecordEntity> findByExternalIdInAndRecordTypeIsAndStateIsAndLeaderRecordStatusNot(Set<UUID> ids, String recordType, String state, Character leaderRecordStatus);
  // To find non-deleted and non-suppressed
  List<MarcRecordEntity> findByExternalIdInAndRecordTypeIsAndStateIsAndLeaderRecordStatusNotAndSuppressDiscoveryIs(Set<UUID> ids,
      String recordType, String state, Character leaderRecordStatus, Boolean suppressDiscovery);

  // To find all non-suppressed including deleted
  List<MarcRecordEntity> findByExternalIdInAndRecordTypeIsAndSuppressDiscoveryIs(Set<UUID> ids, String recordType, Boolean suppressDiscovery);

  @Cacheable(cacheManager = "cacheManagerPerExport", cacheNames = "deleted-not-suppressed-marc-ids")
  @Query(value = "SELECT external_id FROM v_marc_records_lb WHERE record_type = 'MARC_BIB' AND (state = 'ACTUAL' AND leader_record_status = 'd' OR state = 'DELETED') AND suppress_discovery = false", nativeQuery = true)
  Set<UUID> getUUIDsOfDeletedAndNotSuppressedMarcRecords();

  @Cacheable(cacheManager = "cacheManagerPerExport", cacheNames = "deleted-marc-ids")
  @Query(value = "SELECT external_id FROM v_marc_records_lb WHERE record_type = 'MARC_BIB' AND (state = 'ACTUAL' AND leader_record_status = 'd' OR state = 'DELETED')", nativeQuery = true)
  Set<UUID> getUUIDsOfDeletedMarcRecords();

  @Cacheable(cacheManager = "cacheManagerPerExport", cacheNames = "deleted-not-suppressed-holdings-marc-ids")
  @Query(value = "SELECT external_id FROM v_marc_records_lb WHERE record_type = 'MARC_HOLDING' AND (state = 'ACTUAL' AND leader_record_status = 'd' OR state = 'DELETED') AND suppress_discovery = false", nativeQuery = true)
  Set<UUID> getUUIDsOfDeletedAndNotSuppressedHoldingsMarcRecords();

  @Cacheable(cacheManager = "cacheManagerPerExport", cacheNames = "deleted-holdings-marc-ids")
  @Query(value = "SELECT external_id FROM v_marc_records_lb WHERE record_type = 'MARC_HOLDING' AND (state = 'ACTUAL' AND leader_record_status = 'd' OR state = 'DELETED')", nativeQuery = true)
  Set<UUID> getUUIDsOfDeletedHoldingsMarcRecords();
}
