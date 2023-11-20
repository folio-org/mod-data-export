package org.folio.dataexp.service.export.strategies;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class HoldingsExportStrategy extends AbstractExportStrategy {

  @Override
  public List<String> getGeneratedMarc(Set<UUID> ids, ExportStrategyStatistic exportStatistic) {
    return new ArrayList<>();
  }
}
