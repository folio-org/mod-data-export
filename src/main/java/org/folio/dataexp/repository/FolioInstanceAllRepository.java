package org.folio.dataexp.repository;

import java.util.List;
import java.util.UUID;
import org.folio.dataexp.domain.entity.InstanceEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Repository for {@link InstanceEntity}. */
public interface FolioInstanceAllRepository extends Repository<InstanceEntity, UUID> {

  /** Finds all instances, including suppressed from discovery and set for deletion. */
  @Query(
      value = "SELECT * FROM v_folio_instance_all WHERE id BETWEEN ?1 AND ?2 ORDER BY id ASC",
      nativeQuery = true)
  Slice<InstanceEntity> findFolioInstanceAll(UUID fromId, UUID toId, Pageable page);

  /** Finds all instances, including suppressed from discovery, not set for deletion. */
  @Query(
      value =
          "SELECT * FROM v_folio_all_non_deleted_suppressed WHERE id BETWEEN ?1 AND ?2"
              + " ORDER BY id ASC",
      nativeQuery = true)
  Slice<InstanceEntity> findFolioInstanceAllNonDeletedSuppressed(
      UUID fromId, UUID toId, Pageable page);

  /** Finds only non-deleted instances, suppressed from discovery. */
  @Query(
      value =
          "SELECT * FROM v_folio_instance_all_non_deleted"
              + " WHERE id BETWEEN ?1 AND ?2 ORDER BY id ASC",
      nativeQuery = true)
  Slice<InstanceEntity> findFolioInstanceAllNonDeleted(UUID fromId, UUID toId, Pageable page);

  /** Finds only non-deleted instances, not suppressed from discovery. */
  @Query(
      value =
          "SELECT * FROM v_folio_instance_all_non_deleted_non_suppressed WHERE id"
              + " BETWEEN ?1 AND ?2"
              + " ORDER BY id ASC",
      nativeQuery = true)
  Slice<InstanceEntity> findFolioInstanceAllNonDeletedNonSuppressed(
      UUID fromId, UUID toId, Pageable page);

  /** Finds only deleted instances, not suppressed from discovery. */
  @Query(
      value = "SELECT * FROM v_instance_all_folio_deleted_not_suppressed ORDER BY id ASC",
      nativeQuery = true)
  List<InstanceEntity> findFolioInstanceAllDeletedNonSuppressed();

  /** Finds MARC instances, non-deleted, custom profile. */
  @Query(
      value =
          "SELECT * FROM v_marc_instance_all_non_deleted_custom_profile WHERE id"
              + " BETWEEN ?1 AND ?2"
              + " ORDER BY id ASC",
      nativeQuery = true)
  Slice<InstanceEntity> findMarcInstanceAllNonDeletedCustomInstanceProfile(
      UUID fromId, UUID toId, Pageable page);

  /** Finds MARC instances, non-deleted, not suppressed, custom profile. */
  @Query(
      value =
          "SELECT *"
              + " FROM v_marc_instance_all_non_deleted_non_suppressed_custom_instance_profile"
              + " WHERE id BETWEEN ?1 AND ?2 ORDER BY id ASC",
      nativeQuery = true)
  Slice<InstanceEntity> findMarcInstanceAllNonDeletedNonSuppressedForCustomInstanceProfile(
      UUID fromId, UUID toId, Pageable page);

  /** Finds MARC instances, deleted, custom profile. */
  @Query(
      value = "SELECT * FROM v_instance_all_marc_deleted_custom_instance_profile ORDER BY id ASC",
      nativeQuery = true)
  List<InstanceEntity> findMarcInstanceAllDeletedForCustomInstanceProfile();

  /** Finds MARC instances, deleted, not suppressed, custom profile. */
  @Query(
      value =
          "SELECT * FROM v_instance_all_marc_deleted_not_suppressed_custom_instance_profile"
              + " ORDER BY id ASC",
      nativeQuery = true)
  List<InstanceEntity> findMarcInstanceAllDeletedNonSuppressedCustomInstanceProfile();
}
