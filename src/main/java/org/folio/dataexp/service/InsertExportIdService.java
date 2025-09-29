package org.folio.dataexp.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for batch inserting export IDs.
 */
@Component
@RequiredArgsConstructor
public class InsertExportIdService {

  private final ExportIdEntityRepository exportIdEntityRepository;

  /**
   * Saves a batch of export IDs to the repository.
   *
   * @param exportIds List of ExportIdEntity objects.
   */
  @Transactional
  public void saveBatch(List<ExportIdEntity> exportIds) {
    for (var exportId : exportIds) {
      exportIdEntityRepository.insertExportId(exportId.getJobExecutionId(),
          exportId.getInstanceId());
    }
  }
}
