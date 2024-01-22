package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.InstanceDeletedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface InstanceEntityDeletedRepository extends JpaRepository<InstanceDeletedEntity, UUID> {

  @Query(value = "SELECT id, jsonb FROM v_instance_all_deleted where jsonb -> 'record' ->> 'discoverySuppress' is null OR jsonb -> 'record' ->> 'discoverySuppress' = 'false'", nativeQuery = true)
  List<InstanceDeletedEntity> findAllDeletedWhenSkipDiscoverySuppressed();
}
