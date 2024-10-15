package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.InstanceEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface FolioInstanceAllRepository extends Repository<InstanceEntity, UUID> {
  // all instances, including suppressed from discovery and set for deletion
  @Query(value = "SELECT * FROM v_folio_instance_all WHERE id BETWEEN ?1 AND ?2 ORDER BY id ASC", nativeQuery = true)
  Slice<InstanceEntity> findFolioInstanceAll(UUID fromId, UUID toId, Pageable page);

  // all instances, including and set for deletion, not suppressed from discovery
  @Query(value = "SELECT * FROM v_folio_all_deleted_non_suppressed WHERE id BETWEEN ?1 AND ?2 ORDER BY id ASC", nativeQuery = true)
  Slice<InstanceEntity> findFolioInstanceAllDeletedNonSuppressed(UUID fromId, UUID toId, Pageable page);

  // all instances, including suppressed from discovery, not set for deletion
  @Query(value = "SELECT * FROM v_folio_all_non_deleted_suppressed WHERE id BETWEEN ?1 AND ?2 ORDER BY id ASC", nativeQuery = true)
  Slice<InstanceEntity> findFolioInstanceAllNonDeletedSuppressed(UUID fromId, UUID toId, Pageable page);

  // onlyNonDeleted, suppressedFromDiscovery = true
  @Query(value = "SELECT * FROM v_folio_instance_all_non_deleted WHERE id BETWEEN ?1 AND ?2 ORDER BY id ASC", nativeQuery = true)
  Slice<InstanceEntity> findFolioInstanceAllNonDeleted(UUID fromId, UUID toId, Pageable page);

  // onlyNonDeleted, suppressedFromDiscovery = false
  @Query(value = "SELECT * FROM v_folio_instance_all_non_deleted_non_suppressed WHERE id BETWEEN ?1 AND ?2 ORDER BY id ASC", nativeQuery = true)
  Slice<InstanceEntity> findFolioInstanceAllNonDeletedNonSuppressed(UUID fromId, UUID toId, Pageable page);

  // onlyDeleted, suppressedFromDiscovery = false
  @Query(value = "SELECT * FROM v_instance_all_folio_deleted_not_suppressed ORDER BY id ASC", nativeQuery = true)
  List<InstanceEntity> findFolioInstanceAllDeletedNonSuppressed();

  // v_marc_instance_all_non_deleted for instance custom profile
  @Query(value = "SELECT * FROM v_marc_instance_all_non_deleted_custom_profile WHERE id BETWEEN ?1 AND ?2 ORDER BY id ASC", nativeQuery = true)
  Slice<InstanceEntity> findMarcInstanceAllNonDeletedCustomInstanceProfile(UUID fromId, UUID toId, Pageable page);

  // v_marc_instance_all_non_deleted_non_suppressed
  @Query(value = "SELECT * FROM v_marc_instance_all_non_deleted_non_suppressed_custom_instance_profile WHERE id BETWEEN ?1 AND ?2 ORDER BY id ASC", nativeQuery = true)
  Slice<InstanceEntity> findMarcInstanceAllNonDeletedNonSuppressedForCustomInstanceProfile(UUID fromId, UUID toId, Pageable page);

  // v_instance_all_marc_deleted
  @Query(value = "SELECT * FROM v_instance_all_marc_deleted_custom_instance_profile ORDER BY id ASC", nativeQuery = true)
  List<InstanceEntity> findMarcInstanceAllDeletedForCustomInstanceProfile();

  // v_instance_all_marc_deleted_not_suppressed
  @Query(value = "SELECT * FROM v_instance_all_marc_deleted_not_suppressed_custom_instance_profile ORDER BY id ASC", nativeQuery = true)
  List<InstanceEntity> findMarcInstanceAllDeletedNonSuppressedCustomInstanceProfile();
}
