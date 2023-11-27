package org.folio.dataexp.service.export;

import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.service.export.strategies.HoldingsExportStrategy;
import org.folio.dataexp.service.export.strategies.InstancesExportStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ExportStrategyFactoryTest {

  @Mock
  private HoldingsExportStrategy holdingsExportStrategy;
  @Mock
  private InstancesExportStrategy instancesExportStrategy;

  @InjectMocks
  private ExportStrategyFactory exportStrategyFactory;

  @Test
  void getExportStrategyTest() {
    var strategy = exportStrategyFactory.getExportStrategy(ExportRequest.IdTypeEnum.HOLDING);
    assertTrue(strategy instanceof HoldingsExportStrategy);

    strategy =  exportStrategyFactory.getExportStrategy(ExportRequest.IdTypeEnum.INSTANCE);
    assertTrue(strategy instanceof InstancesExportStrategy);
  }
}
