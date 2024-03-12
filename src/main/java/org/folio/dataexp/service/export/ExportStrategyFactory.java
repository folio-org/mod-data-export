package org.folio.dataexp.service.export;

import lombok.AllArgsConstructor;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.service.export.strategies.AuthorityExportAllStrategy;
import org.folio.dataexp.service.export.strategies.AuthorityExportStrategy;
import org.folio.dataexp.service.export.strategies.ExportStrategy;
import org.folio.dataexp.service.export.strategies.HoldingsExportAllStrategy;
import org.folio.dataexp.service.export.strategies.HoldingsExportStrategy;
import org.folio.dataexp.service.export.strategies.InstancesExportAllStrategy;
import org.folio.dataexp.service.export.strategies.InstancesExportStrategy;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ExportStrategyFactory {

  private final HoldingsExportStrategy holdingsExportStrategy;
  private final InstancesExportStrategy instancesExportStrategy;
  private final AuthorityExportStrategy authorityExportStrategy;
  private final InstancesExportAllStrategy instancesExportAllStrategy;
  private final HoldingsExportAllStrategy holdingsExportAllStrategy;
  private final AuthorityExportAllStrategy authorityExportAllStrategy;

  public ExportStrategy getExportStrategy(ExportRequest exportRequest) {
    if (exportRequest.getIdType() == ExportRequest.IdTypeEnum.HOLDING) {
      if (Boolean.TRUE.equals(exportRequest.getAll())) {
        return holdingsExportAllStrategy;
      }
      return holdingsExportStrategy;
    } else if (exportRequest.getIdType() == ExportRequest.IdTypeEnum.AUTHORITY) {
      if (Boolean.TRUE.equals(exportRequest.getAll())) {
        return authorityExportAllStrategy;
      }
      return authorityExportStrategy;
    }
    if (Boolean.TRUE.equals(exportRequest.getAll())) {
      return instancesExportAllStrategy;
    }
    return instancesExportStrategy;
  }

}
