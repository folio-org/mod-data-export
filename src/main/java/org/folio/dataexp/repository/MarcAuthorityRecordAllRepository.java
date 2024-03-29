package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface MarcAuthorityRecordAllRepository extends Repository<MarcRecordEntity, UUID> {

  @Query(value = "SELECT id, content, external_id, record_type, state, leader_record_status, suppress_discovery, generation "
      + "FROM v_authority_all "
      + "WHERE state = 'ACTUAL' AND leader_record_status != 'd' "
      + "AND external_id BETWEEN ?1 AND ?2 "
      + "ORDER BY id ASC", nativeQuery = true)
  Slice<MarcRecordEntity> findAllWithoutDeleted(UUID fromId, UUID toId, Pageable page);

  @Query(value = "SELECT id, content, external_id, record_type, state, leader_record_status, suppress_discovery, generation "
      + "FROM v_authority_all "
      + "WHERE external_id BETWEEN ?1 AND ?2 "
      + "ORDER BY id ASC", nativeQuery = true)
  Slice<MarcRecordEntity> findAllWithDeleted(UUID fromId, UUID toId, Pageable page);

  @Query(value = "SELECT COUNT(id) FROM v_authority_all", nativeQuery = true)
  long count();
}
