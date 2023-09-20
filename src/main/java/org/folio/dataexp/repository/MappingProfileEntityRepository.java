package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MappingProfileEntityRepository extends JpaRepository<MappingProfileEntity, UUID> {
}
