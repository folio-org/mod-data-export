package org.folio.dataexp.service.export.strategies;

import lombok.extern.log4j.Log4j2;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.service.export.storage.FolioS3ClientFactory;
import org.folio.s3.client.RemoteStorageWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.minidev.json.parser.JSONParser.DEFAULT_PERMISSIVE_MODE;
import static org.folio.dataexp.service.export.Constants.OUTPUT_BUFFER_SIZE;

@Log4j2
public abstract class AbstractExportStrategy implements ExportStrategy {

  private int exportIdsBatch;
  private FolioS3ClientFactory folioS3ClientFactory;
  private JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  private ExportIdEntityRepository exportIdEntityRepository;

  private MappingProfileEntityRepository mappingProfileEntityRepository;
  private JobProfileEntityRepository jobProfileEntityRepository;
  private JobExecutionEntityRepository jobExecutionEntityRepository;
  private JsonToMarcConverter jsonToMarcConverter;

  @Value("#{ T(Integer).parseInt('${application.export-ids-batch}')}")
  private void setExportIdsBatch(int exportIdsBatch) {
    this.exportIdsBatch = exportIdsBatch;
  }

  @Autowired
  private void setFolioS3ClientFactory(FolioS3ClientFactory folioS3ClientFactory) {
    this.folioS3ClientFactory = folioS3ClientFactory;
  }

  @Autowired
  private void setJobExecutionExportFilesEntityRepository(JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository) {
    this.jobExecutionExportFilesEntityRepository = jobExecutionExportFilesEntityRepository;
  }

  @Autowired
  private void setExportIdEntityRepository(ExportIdEntityRepository exportIdEntityRepository) {
    this.exportIdEntityRepository = exportIdEntityRepository;
  }

  @Autowired
  private void setJsonToMarcConverter(JsonToMarcConverter jsonToMarcConverter) {
    this.jsonToMarcConverter = jsonToMarcConverter;
  }

  @Autowired
  private void setMappingProfileEntityRepository(MappingProfileEntityRepository mappingProfileEntityRepository) {
    this.mappingProfileEntityRepository = mappingProfileEntityRepository;
  }

  @Autowired
  private void setJobProfileEntityRepository(JobProfileEntityRepository jobProfileEntityRepository) {
    this.jobProfileEntityRepository = jobProfileEntityRepository;
  }

  @Autowired
  private void setJobExecutionEntityRepository(JobExecutionEntityRepository jobExecutionEntityRepository) {
    this.jobExecutionEntityRepository = jobExecutionEntityRepository;
  }

  @Override
  public ExportStrategyStatistic saveMarcToRemoteStorage(JobExecutionExportFilesEntity exportFilesEntity) {
    var exportStatistic = new ExportStrategyStatistic();
    var mappingProfile = getMappingProfile(exportFilesEntity.getJobExecutionId());
    var s3Client = folioS3ClientFactory.getFolioS3Client();
    var remoteStorageWriter = new RemoteStorageWriter(exportFilesEntity.getFileLocation(), OUTPUT_BUFFER_SIZE, s3Client);
    var slice = exportIdEntityRepository.getExportIds(exportFilesEntity.getJobExecutionId(),
      exportFilesEntity.getFromId(), exportFilesEntity.getToId(), PageRequest.of(0, exportIdsBatch));
    var exportIds = slice.getContent().stream().map(ExportIdEntity::getInstanceId).collect(Collectors.toSet());
    createAndSaveMarc(exportIds, remoteStorageWriter, exportStatistic, mappingProfile);
    while (slice.hasNext()) {
      slice = exportIdEntityRepository.getExportIds(exportFilesEntity.getJobExecutionId(),
        exportFilesEntity.getFromId(), exportFilesEntity.getToId(), slice.nextPageable());
      exportIds = slice.getContent().stream().map(ExportIdEntity::getInstanceId).collect(Collectors.toSet());
      createAndSaveMarc(exportIds, remoteStorageWriter, exportStatistic, mappingProfile);
    }
    try {
      remoteStorageWriter.close();
      exportFilesEntity.setStatusBaseExportStatistic(exportStatistic);
      jobExecutionExportFilesEntityRepository.save(exportFilesEntity);
    } catch (Exception e) {
      log.error("saveMarcToRemoteStorage:: Error while uploading file {} to remote storage for job execution {}", exportFilesEntity.getFileLocation(), exportFilesEntity.getJobExecutionId());
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

  abstract List<MarcRecordEntity> getMarcRecords(Set<UUID> externalIds);

  abstract List<String> getGeneratedMarc(Set<UUID> ids, ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile);

  protected Optional<JSONObject> getAsJsonObject(String jsonAsString) {
    try {
      var jsonParser = new JSONParser(DEFAULT_PERMISSIVE_MODE);
      return Optional.of((JSONObject) jsonParser.parse(jsonAsString));
    } catch (ParseException e) {
      log.error("getAsJsonObject:: Error converting string to json {}", e.getMessage());
    }
    return Optional.empty();
  }

  private void createAndSaveMarc(Set<UUID> externalIds, RemoteStorageWriter remoteStorageWriter,
                                 ExportStrategyStatistic exportStatistic, MappingProfile mappingProfile) {
    var marcRecords = getMarcRecords(externalIds);
    var externalIdsWithMarcRecord = new HashSet<UUID>();
    for (var marcRecordEntity : marcRecords) {
      var marc = StringUtils.EMPTY;
      try {
        marc = jsonToMarcConverter.convertJsonRecordToMarcRecord(marcRecordEntity.getContent());
      } catch (Exception e) {
        log.error("Error converting json to marc for record {}", marcRecordEntity.getExternalId());
        exportStatistic.incrementFailed();
        continue;
      }
      remoteStorageWriter.write(marc);
      if (externalIdsWithMarcRecord.contains(marcRecordEntity.getExternalId())) {
        exportStatistic.incrementDuplicatedSrs();
      } else {
        externalIdsWithMarcRecord.add(marcRecordEntity.getExternalId());
      }
      exportStatistic.incrementExported();
    }
    marcRecords.clear();
    externalIds.removeAll(externalIdsWithMarcRecord);
    var generatedMarc = getGeneratedMarc(externalIds, exportStatistic, mappingProfile);
    generatedMarc.forEach(marc -> {
      remoteStorageWriter.write(marc);
      exportStatistic.incrementExported();
      }
    );
  }

  private MappingProfile getMappingProfile(UUID jobExecutionId) {
    var jobExecution = jobExecutionEntityRepository.getReferenceById(jobExecutionId);
    var jobProfile = jobProfileEntityRepository.getReferenceById(jobExecution.getJobProfileId());
    return mappingProfileEntityRepository.getReferenceById(jobProfile.getMappingProfileId()).getMappingProfile();
  }
}
