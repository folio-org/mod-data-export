package org.folio.dataexp.service.export.strategies.ld;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.LinkedDataResource;
import org.springframework.stereotype.Component;

/** Concrete implementation of abstract Linked Data strategy, using the LinkedDataProvider. */
@Log4j2
@Component
@RequiredArgsConstructor
public class LinkedDataExportStrategy extends AbstractLinkedDataExportStrategy {

  private final LinkedDataProvider linkedDataProvider;

  /**
   * Retrieve a set of Linked Data resources given a set of instance IDs.
   *
   * @param externalIds set of instance identifiers
   * @return a list of Linked Data resource JSON as string
   */
  @Override
  List<LinkedDataResource> getLinkedDataResources(Set<UUID> externalIds) {
    return linkedDataProvider.getLinkedDataResources(externalIds);
  }
}
