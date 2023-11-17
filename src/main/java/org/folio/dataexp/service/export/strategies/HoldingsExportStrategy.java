package org.folio.dataexp.service.export.strategies;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.repository.MarcRecordEntityRepository;
import org.folio.dataexp.service.export.storage.FolioS3ClientFactory;
import org.folio.s3.client.RemoteStorageWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.folio.dataexp.service.export.Constants.OUTPUT_BUFFER_SIZE;

@Log4j2
@Component
public class HoldingsExportStrategy implements ExportStrategy {

  @Value("#{ T(Integer).parseInt('${application.export-ids-batch}')}")
  private int exportIdsBatch;

  @Autowired
  private FolioS3ClientFactory folioS3ClientFactory;
  @Autowired
  private JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  @Autowired
  private ExportIdEntityRepository exportIdEntityRepository;
  @Autowired
  private MarcRecordEntityRepository marcRecordEntityRepository;
  @Autowired
  private JsonToMarcConverter jsonToMarcConverter;

  @Override
  public ExportStrategyStatistic saveMarcToRemoteStorage(JobExecutionExportFilesEntity exportFilesEntity) {
    var exportStatistic = new ExportStrategyStatistic();
    var s3Client = folioS3ClientFactory.getFolioS3Client();
    var remoteStorageWriter = new RemoteStorageWriter(exportFilesEntity.getFileLocation(),  OUTPUT_BUFFER_SIZE, s3Client);
    var slice = exportIdEntityRepository.getExportIds(exportFilesEntity.getJobExecutionId(),
      exportFilesEntity.getFromId(), exportFilesEntity.getToId(), PageRequest.of(0, exportIdsBatch));
    var exportIds = slice.getContent().stream().map(ExportIdEntity::getInstanceId).collect(Collectors.toSet());
    createAndSaveMarc(exportIds, remoteStorageWriter, exportStatistic);
    while (slice.hasNext()) {
      slice = exportIdEntityRepository.getExportIds(exportFilesEntity.getJobExecutionId(),
        exportFilesEntity.getFromId(), exportFilesEntity.getToId(),slice.nextPageable());
      exportIds = slice.getContent().stream().map(ExportIdEntity::getInstanceId).collect(Collectors.toSet());
      createAndSaveMarc(exportIds, remoteStorageWriter, exportStatistic);
    }
    try {
      remoteStorageWriter.close();
      exportFilesEntity.setStatusBaseExportStatistic(exportStatistic);
      jobExecutionExportFilesEntityRepository.save(exportFilesEntity);
    } catch (Exception e) {
      log.error("Error while uploading file {} to remote storage for job execution {}", exportFilesEntity.getFileLocation(), exportFilesEntity.getJobExecutionId());
      exportStatistic.setDuplicatedSrs(0);
      exportStatistic.setExported(0);
      long countFailed = exportIdEntityRepository.countExportIds(exportFilesEntity.getJobExecutionId(),
        exportFilesEntity.getFromId(), exportFilesEntity.getToId());
      exportStatistic.setFailed((int) countFailed);
      exportFilesEntity.setStatusBaseExportStatistic(exportStatistic);
      jobExecutionExportFilesEntityRepository.save(exportFilesEntity);
    }
    return exportStatistic;
  }

  private void createAndSaveMarc(Set<UUID> externalIds, RemoteStorageWriter remoteStorageWriter, ExportStrategyStatistic exportStatistic) {
    var marcRecords = marcRecordEntityRepository.findByExternalIdIn(externalIds);
    var idsWithMarcRecord = new HashSet<UUID>();
    for (var marcRecordEntity : marcRecords) {
      var marc = StringUtils.EMPTY;
      try {
        marc = jsonToMarcConverter.convertJsonRecordToMarcRecord(marcRecordEntity.getContent());
      } catch (Exception e) {
        log.error("Error converting json to marc of record {}", marcRecordEntity.getExternalId());
        exportStatistic.setFailed(exportStatistic.getFailed() + 1);
        continue;
      }
      remoteStorageWriter.write(marc);
      if (idsWithMarcRecord.contains(marcRecordEntity.getId())) {
        exportStatistic.setDuplicatedSrs(exportStatistic.getDuplicatedSrs() + 1);
      } else {
        idsWithMarcRecord.add(marcRecordEntity.getId());
      }
      exportStatistic.setExported(exportStatistic.getExported() + 1);
    }
    externalIds.removeAll(idsWithMarcRecord);
    generateMarc(externalIds, remoteStorageWriter, exportStatistic);
  }


  private void generateMarc(Set<UUID> ids, RemoteStorageWriter remoteStorageWriter, ExportStrategyStatistic exportStatistic) {

  }
}
