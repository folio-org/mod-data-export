package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.InstanceWithHridEntity;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface InstanceWithHridEntityRepository extends Repository<InstanceWithHridEntity, UUID> {

  List<InstanceWithHridEntity> findByIdIn(Set<UUID> ids);
}
