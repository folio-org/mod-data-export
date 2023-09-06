package org.folio.dataexp.service.export.strategies;

import org.apache.commons.io.FileUtils;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

@Component
public class InstancesExportStrategy implements ExportStrategy {

  @Override
  public ExportStrategyStatistic saveMarc(JobExecutionExportFilesEntity exportFilesEntity, File file) {
    var marc = "marc";
    try {
      FileUtils.writeStringToFile(file, marc, Charset.defaultCharset());
      var exportStatistic = new ExportStrategyStatistic();
      exportStatistic.setTotal(1);
      exportStatistic.setExported(1);
      return exportStatistic;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
