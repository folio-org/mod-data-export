package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InsertExportIdService {

  private final ExportIdEntityRepository exportIdEntityRepository;

  @Transactional
  public void saveBatch(List<ExportIdEntity> exportIds) {
    for (var exportId : exportIds) {
      exportIdEntityRepository.insertExportId(exportId.getJobExecutionId(), exportId.getInstanceId());
    }
  }
}
