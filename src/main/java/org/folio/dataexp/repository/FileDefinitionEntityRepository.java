package org.folio.dataexp.repository;

import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FileDefinitionEntityRepository extends JpaRepository<FileDefinitionEntity, UUID> {
}
