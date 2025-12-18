package org.folio.dataexp.repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Repository for {@link MarcRecordEntity}. */
public interface MarcRecordEntityRepository extends Repository<MarcRecordEntity, UUID> {

  /**
   * Finds MARC records by external IDs, record type, and states.
   *
   * @param ids set of external UUIDs
   * @param recordType MARC record type
   * @param states set of states
   * @return list of MarcRecordEntity
   */
  @Transactional
  List<MarcRecordEntity> findByExternalIdInAndRecordTypeIsAndStateIn(
      Set<UUID> ids, String recordType, Set<String> states);
}
