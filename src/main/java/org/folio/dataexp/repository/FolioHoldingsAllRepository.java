package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.HoldingsRecordEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface FolioHoldingsAllRepository extends Repository<HoldingsRecordEntity, UUID> {

  // onlyNonDeleted, suppressedFromDiscovery = true
  @Query(value = "SELECT * FROM v_folio_holdings_all_non_deleted WHERE id BETWEEN ?1 AND ?2 ORDER BY id ASC", nativeQuery = true)
  Slice<HoldingsRecordEntity> findFolioHoldingsAllNonDeleted(UUID fromId, UUID toId, Pageable page);

  // onlyNonDeleted, suppressedFromDiscovery = false
  @Query(value = "SELECT * FROM v_folio_holdings_all_non_deleted_non_suppressed WHERE id BETWEEN ?1 AND ?2 ORDER BY id ASC", nativeQuery = true)
  Slice<HoldingsRecordEntity> findFolioHoldingsAllNonDeletedNonSuppressed(UUID fromId, UUID toId, Pageable page);

  // onlyDeleted, suppressedFromDiscovery = true
  @Query(value = "SELECT * FROM v_holdings_all_folio_deleted ORDER BY id ASC", nativeQuery = true)
  List<HoldingsRecordEntity> findFolioHoldingsAllDeleted();

  // onlyDeleted, suppressedFromDiscovery = false
  @Query(value = "SELECT * FROM v_holdings_all_folio_deleted_not_suppressed ORDER BY id ASC", nativeQuery = true)
  List<HoldingsRecordEntity> findFolioHoldingsAllDeletedNonSuppressed();

  @Query(value = "SELECT * FROM v_marc_holdings_all_non_deleted_custom_profile WHERE id BETWEEN ?1 AND ?2 ORDER BY id ASC", nativeQuery = true)
  Slice<HoldingsRecordEntity> findMarcHoldingsAllNonDeletedCustomHoldingsProfile(UUID fromId, UUID toId, Pageable page);

  @Query(value = "SELECT * FROM v_marc_holdings_all_non_deleted_non_suppressed_custom_profile WHERE id BETWEEN ?1 AND ?2 ORDER BY id ASC", nativeQuery = true)
  Slice<HoldingsRecordEntity> findMarcHoldingsAllNonDeletedNonSuppressedCustomHoldingsProfile(UUID fromId, UUID toId, Pageable page);

  @Query(value = "SELECT * FROM v_holdings_all_marc_deleted_custom_profile ORDER BY id ASC", nativeQuery = true)
  List<HoldingsRecordEntity> findMarcHoldingsAllDeletedCustomHoldingsProfile();

  @Query(value = "SELECT * FROM v_holdings_all_marc_deleted_not_suppressed_custom_profile ORDER BY id ASC", nativeQuery = true)
  List<HoldingsRecordEntity> findMarcHoldingsAllDeletedNonSuppressedCustomHoldingsProfile();
}
