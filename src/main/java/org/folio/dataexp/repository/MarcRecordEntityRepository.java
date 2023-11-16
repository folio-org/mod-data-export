package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;
import java.util.UUID;

public interface MarcRecordEntityRepository extends JpaRepository<MarcRecordEntity, UUID> {

  Slice<MarcRecordEntity> findByIdIn(Set<UUID> ids, Pageable page);
}
