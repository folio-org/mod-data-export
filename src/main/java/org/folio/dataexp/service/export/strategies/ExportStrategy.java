package org.folio.dataexp.service.export.strategies;

import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;

import java.io.File;

public interface ExportStrategy {

  void saveMarc(JobExecutionExportFilesEntity exportFilesEntity, File file);

}
