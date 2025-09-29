package org.folio.dataexp.repository;

import java.util.UUID;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Repository for {@link MarcRecordEntity} authority records.
 */
public interface MarcAuthorityRecordAllRepository
    extends Repository<MarcRecordEntity, UUID> {

  /**
   * Finds all authority records without deleted.
   *
   * @param fromId start external UUID
   * @param toId end external UUID
   * @param page pageable
   * @return slice of MarcRecordEntity
   */
  @Query(
      value =
      "SELECT id, content, external_id, record_type, state, leader_record_status,"
        + " suppress_discovery, generation"
        + " FROM v_authority_all"
        + " WHERE state = 'ACTUAL'"
        + " AND leader_record_status != 'd'"
        + " AND external_id BETWEEN ?1 AND ?2"
        + " ORDER BY id ASC",
      nativeQuery = true
  )
  Slice<MarcRecordEntity> findAllWithoutDeleted(
      UUID fromId,
      UUID toId,
      Pageable page
  );

  /**
   * Finds all authority records including deleted.
   *
   * @param fromId start external UUID
   * @param toId end external UUID
   * @param page pageable
   * @return slice of MarcRecordEntity
   */
  @Query(
      value =
      "SELECT id, content, external_id, record_type, state, leader_record_status,"
        + " suppress_discovery, generation"
        + " FROM v_authority_all"
        + " WHERE external_id BETWEEN ?1 AND ?2"
        + " ORDER BY id ASC",
      nativeQuery = true
  )
  Slice<MarcRecordEntity> findAllWithDeleted(
      UUID fromId,
      UUID toId,
      Pageable page
  );

  /**
   * Counts all authority records.
   *
   * @return count of records
   */
  @Query(
      value = "SELECT COUNT(id) FROM v_authority_all",
      nativeQuery = true
  )
  long count();
}
