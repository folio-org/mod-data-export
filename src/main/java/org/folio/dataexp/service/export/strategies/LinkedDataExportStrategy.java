package org.folio.dataexp.service.export.strategies;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;

public class LinkedDataExportStrategy extends AbstractLinkedDataExportStrategy {

  private LinkedDataProvider linkedDataProvider;

  @Override
  List<String> getLinkedDataResources(Set<UUID> externalIds, MappingProfile mappingProfile,
      ExportRequest exportRequest, UUID jobExecutionId) {
    // TODO: implement - call on linkedDataProvider
    return Collections.emptyList();
  }
}
