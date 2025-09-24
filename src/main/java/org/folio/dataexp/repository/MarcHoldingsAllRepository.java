package org.folio.dataexp.repository;

import java.util.List;
import java.util.UUID;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Repository for {@link MarcRecordEntity} MARC holdings.
 */
public interface MarcHoldingsAllRepository
    extends Repository<MarcRecordEntity, UUID> {

  /**
   * Finds all non-deleted MARC holdings.
   *
   * @param fromId start external UUID
   * @param toId end external UUID
   * @param page pageable
   * @return slice of MarcRecordEntity
   */
  @Query(
      value =
      "SELECT * FROM v_marc_holdings_all_non_deleted"
        + " WHERE external_id BETWEEN ?1 AND ?2"
        + " ORDER BY id ASC",
      nativeQuery = true
  )
  Slice<MarcRecordEntity> findMarcHoldingsAllNonDeleted(
      UUID fromId,
      UUID toId,
      Pageable page
  );

  /**
   * Finds all non-deleted, non-suppressed MARC holdings.
   *
   * @param fromId start external UUID
   * @param toId end external UUID
   * @param page pageable
   * @return slice of MarcRecordEntity
   */
  @Query(
      value =
      "SELECT * FROM v_marc_holdings_all_non_deleted_non_suppressed"
        + " WHERE external_id BETWEEN ?1 AND ?2"
        + " ORDER BY id ASC",
      nativeQuery = true
  )
  Slice<MarcRecordEntity> findMarcHoldingsAllNonDeletedNonSuppressed(
      UUID fromId,
      UUID toId,
      Pageable page
  );

  /**
   * Finds all deleted MARC holdings.
   *
   * @return list of MarcRecordEntity
   */
  @Query(
      value = "SELECT * FROM v_holdings_all_marc_deleted ORDER BY id ASC",
      nativeQuery = true
  )
  List<MarcRecordEntity> findMarcHoldingsAllDeleted();

  /**
   * Finds all deleted, non-suppressed MARC holdings.
   *
   * @return list of MarcRecordEntity
   */
  @Query(
      value = "SELECT * FROM v_holdings_all_marc_deleted_not_suppressed ORDER BY id ASC",
      nativeQuery = true
  )
  List<MarcRecordEntity> findMarcHoldingsAllDeletedNonSuppressed();
}
