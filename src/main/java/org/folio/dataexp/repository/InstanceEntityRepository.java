package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.InstanceEntity;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface InstanceEntityRepository extends Repository<InstanceEntity, UUID> {

  List<InstanceEntity> findByIdIn(Set<UUID> ids);
}
