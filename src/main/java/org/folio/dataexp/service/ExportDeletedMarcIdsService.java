package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportDeletedMarcIdsRequest;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Log4j2
@RequiredArgsConstructor
@Service
public class ExportDeletedMarcIdsService {

  private final MarcDeletedIdsService marcDeletedIdsService;
  private final DataExportService dataExportService;
  private final JobProfileEntityRepository jobProfileEntityRepository;

  public void postExportDeletedMarcIds(ExportDeletedMarcIdsRequest request) {
    log.info("POST export deleted MARC IDs");
    var fileDefinition = marcDeletedIdsService.getFileDefinitionForMarcDeletedIds(request.getFrom(), request.getTo());
    var defaultJobProfileId = getDefaultJobProfileId();
    var exportRequest = ExportRequest.builder().fileDefinitionId(fileDefinition.getId()).jobProfileId(defaultJobProfileId)
      .build();
    dataExportService.postDataExport(exportRequest);
  }

  private UUID getDefaultJobProfileId() {
    return jobProfileEntityRepository.findIdOfDefaultJobProfileByName(ExportRequest.IdTypeEnum.INSTANCE.getValue()).get(0);
  }
}
