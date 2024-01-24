package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.InstanceEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface InstanceEntityRepository extends Repository<InstanceEntity, UUID> {

  List<InstanceEntity> findByIdIn(Set<UUID> ids);

  Slice<InstanceEntity> findByIdGreaterThanEqualAndIdLessThanEqualOrderByIdAsc(UUID fromId, UUID toId, Pageable page);

  @Query(value = "SELECT * FROM v_instance_all WHERE id BETWEEN ?1 AND ?2 AND (jsonb ->> 'discoverySuppress' is null OR jsonb ->> 'discoverySuppress' = 'false') ORDER BY id ASC", nativeQuery = true)
  Slice<InstanceEntity> findAllWhenSkipDiscoverySuppressed(UUID fromId, UUID toId, Pageable page);
}
