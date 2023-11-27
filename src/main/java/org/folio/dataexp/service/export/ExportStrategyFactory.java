package org.folio.dataexp.service.export;

import lombok.AllArgsConstructor;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.service.export.strategies.ExportStrategy;
import org.folio.dataexp.service.export.strategies.HoldingsExportStrategy;
import org.folio.dataexp.service.export.strategies.InstancesExportStrategy;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ExportStrategyFactory {

  private final HoldingsExportStrategy holdingsExportStrategy;
  private final InstancesExportStrategy instancesExportStrategy;

  public ExportStrategy getExportStrategy(ExportRequest.IdTypeEnum idType) {
    if (idType == ExportRequest.IdTypeEnum.HOLDING) return holdingsExportStrategy;
    return instancesExportStrategy;
  }

}