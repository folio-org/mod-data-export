package org.folio.dataexp.service.export;

import lombok.AllArgsConstructor;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.IdType;
import org.folio.dataexp.service.export.strategies.AbstractMarcExportStrategy;
import org.folio.dataexp.service.export.strategies.AuthorityExportAllStrategy;
import org.folio.dataexp.service.export.strategies.AuthorityExportStrategy;
import org.folio.dataexp.service.export.strategies.ExportStrategy;
import org.folio.dataexp.service.export.strategies.HoldingsExportAllStrategy;
import org.folio.dataexp.service.export.strategies.HoldingsExportStrategy;
import org.folio.dataexp.service.export.strategies.InstancesExportAllStrategy;
import org.folio.dataexp.service.export.strategies.InstancesExportStrategy;
import org.folio.dataexp.service.export.strategies.ld.LinkedDataExportStrategy;
import org.springframework.stereotype.Component;

/**
 * Factory for selecting the appropriate export strategy based on request or record type.
 */
@Component
@AllArgsConstructor
public class ExportStrategyFactory {

  private final HoldingsExportStrategy holdingsExportStrategy;
  private final InstancesExportStrategy instancesExportStrategy;
  private final AuthorityExportStrategy authorityExportStrategy;
  private final LinkedDataExportStrategy linkedDataExportStrategy;
  private final InstancesExportAllStrategy instancesExportAllStrategy;
  private final HoldingsExportAllStrategy holdingsExportAllStrategy;
  private final AuthorityExportAllStrategy authorityExportAllStrategy;

  /**
   * Gets the export strategy for the given export request.
   *
   * @param exportRequest the export request
   * @return the export strategy
   */
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
    if (exportRequest.getRecordType() == ExportRequest.RecordTypeEnum.LINKED_DATA) {
      return linkedDataExportStrategy;
    }
    if (Boolean.TRUE.equals(exportRequest.getAll())) {
      return instancesExportAllStrategy;
    }
    return instancesExportStrategy;
  }

  /**
   * Gets the export strategy for the given record ID type.
   *
   * @param recordIdType the record ID type
   * @return the abstract export strategy
   */
  public AbstractMarcExportStrategy getExportStrategy(IdType recordIdType) {
    return switch (recordIdType) {
      case AUTHORITY -> authorityExportStrategy;
      case INSTANCE -> instancesExportStrategy;
    };
  }
}
