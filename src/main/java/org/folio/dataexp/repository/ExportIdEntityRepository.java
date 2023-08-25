package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ExportIdEntityRepository extends JpaRepository<ExportIdEntity, Integer> {
}
