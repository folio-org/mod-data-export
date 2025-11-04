package org.folio.dataexp.service.export;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.ExportRequest.RecordTypeEnum;
import org.folio.dataexp.service.export.strategies.AuthorityExportAllStrategy;
import org.folio.dataexp.service.export.strategies.AuthorityExportStrategy;
import org.folio.dataexp.service.export.strategies.HoldingsExportAllStrategy;
import org.folio.dataexp.service.export.strategies.HoldingsExportStrategy;
import org.folio.dataexp.service.export.strategies.InstancesExportAllStrategy;
import org.folio.dataexp.service.export.strategies.InstancesExportStrategy;
import org.folio.dataexp.service.export.strategies.ld.LinkedDataExportStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportStrategyFactoryTest {

  @Mock
  private HoldingsExportStrategy holdingsExportStrategy;
  @Mock
  private InstancesExportStrategy instancesExportStrategy;
  @Mock
  private AuthorityExportStrategy authorityExportStrategy;
  @Mock
  private HoldingsExportAllStrategy holdingsExportAllStrategy;
  @Mock
  private InstancesExportAllStrategy instancesExportAllStrategy;
  @Mock
  private AuthorityExportAllStrategy authorityExportAllStrategy;
  @Mock
  private LinkedDataExportStrategy linkedDataExportStrategy;

  @InjectMocks
  private ExportStrategyFactory exportStrategyFactory;

  @Test
  void getExportStrategyTest() {
    var strategy = exportStrategyFactory.getExportStrategy(
        new ExportRequest().idType(ExportRequest.IdTypeEnum.HOLDING));
    assertTrue(strategy instanceof HoldingsExportStrategy);

    strategy =  exportStrategyFactory.getExportStrategy(
        new ExportRequest().idType(ExportRequest.IdTypeEnum.INSTANCE));
    assertTrue(strategy instanceof InstancesExportStrategy);

    strategy =  exportStrategyFactory.getExportStrategy(
        new ExportRequest().idType(ExportRequest.IdTypeEnum.AUTHORITY));
    assertTrue(strategy instanceof AuthorityExportStrategy);

    strategy = exportStrategyFactory.getExportStrategy(
        new ExportRequest().idType(ExportRequest.IdTypeEnum.HOLDING).all(true));
    assertTrue(strategy instanceof HoldingsExportAllStrategy);

    strategy =  exportStrategyFactory.getExportStrategy(
        new ExportRequest().idType(ExportRequest.IdTypeEnum.INSTANCE).all(true));
    assertTrue(strategy instanceof InstancesExportAllStrategy);

    strategy =  exportStrategyFactory.getExportStrategy(
        new ExportRequest().idType(ExportRequest.IdTypeEnum.AUTHORITY).all(true));
    assertTrue(strategy instanceof AuthorityExportAllStrategy);

    strategy = exportStrategyFactory.getExportStrategy(
        new ExportRequest()
            .idType(ExportRequest.IdTypeEnum.INSTANCE)
            .recordType(RecordTypeEnum.LINKED_DATA));
    assertTrue(strategy instanceof LinkedDataExportStrategy);
  }
}
