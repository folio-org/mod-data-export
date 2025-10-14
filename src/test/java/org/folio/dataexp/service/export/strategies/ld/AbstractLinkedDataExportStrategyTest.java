package org.folio.dataexp.service.export.strategies.ld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Setter;
import lombok.SneakyThrows;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.dto.LinkedDataResource;
import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.folio.dataexp.exception.export.LocalStorageWriterException;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.service.JobExecutionService;
import org.folio.dataexp.service.export.LocalStorageWriter;
import org.folio.dataexp.service.export.strategies.ExportedRecordsListener;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.s3.client.FolioS3Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

@ExtendWith(MockitoExtension.class)
class AbstractLinkedDataExportStrategyTest {

  @Mock
  private FolioS3Client s3Client;
  @Mock
  private JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  @Mock
  private ExportIdEntityRepository exportIdEntityRepository;
  @Mock
  private JobProfileEntityRepository jobProfileEntityRepository;
  @Mock
  private JobExecutionEntityRepository jobExecutionEntityRepository;
  @Mock
  private JobExecutionService jobExecutionService;
  @Mock
  private LocalStorageWriter localStorageWriter;
  @Mock
  private ErrorLogService errorLogService;
  @Mock
  private LinkedDataConverter linkedDataConverter;

  @InjectMocks
  private AbstractLinkedDataExportStrategy exportStrategy = new LdTestExportStrategy(1);

  @BeforeEach
  void clear() {
    ((LdTestExportStrategy) exportStrategy).setLinkedDataResources(new ArrayList<>());
  }

  @SneakyThrows
  @Test
  void saveOutputToLocalStorageTest() {
    var progress = new JobExecutionProgress();
    var jobExecution = JobExecution.builder().progress(progress).id(UUID.randomUUID()).build();
    var jobProfileEntity = new JobProfileEntity();
    jobProfileEntity.setId(UUID.randomUUID());
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setJobProfileId(jobProfileEntity.getId());

    var linkedDataResources = new ArrayList<LinkedDataResource>();
    var linkedDataResource = new LinkedDataResource();
    var exportId = UUID.randomUUID();
    linkedDataResource.setInventoryId(exportId.toString());
    linkedDataResource.setResource("{}");
    linkedDataResources.add(linkedDataResource);
    ((LdTestExportStrategy) exportStrategy).setLinkedDataResources(linkedDataResources);

    JobExecutionExportFilesEntity exportFilesEntity = new JobExecutionExportFilesEntity()
        .withFileLocation("/tmp/" + jobExecution.getId().toString() + "/location")
        .withId(UUID.randomUUID()).withJobExecutionId(jobExecution.getId())
        .withFromId(UUID.randomUUID()).withToId(UUID.randomUUID())
        .withStatus(JobExecutionExportFilesStatus.ACTIVE);
    var exportIdEntity = new ExportIdEntity()
        .withJobExecutionId(exportFilesEntity.getJobExecutionId())
        .withId(0).withInstanceId(exportId);
    var slice = new SliceImpl<>(List.of(exportIdEntity), PageRequest.of(0, 1), false);

    when(exportIdEntityRepository.getExportIds(isA(UUID.class), isA(UUID.class), isA(UUID.class),
          isA(Pageable.class))).thenReturn(slice);
    var jobExecutionEntity = JobExecutionEntity.fromJobExecution(jobExecution);
    when(jobExecutionEntityRepository.getReferenceById(isA(UUID.class)))
        .thenReturn(jobExecutionEntity);
    var output = new ByteArrayOutputStream(2);
    output.write("{}".getBytes());
    when(linkedDataConverter.convertLdJsonToBibframe2Rdf(isA(String.class)))
        .thenReturn(output);

    var exportStatistic = exportStrategy.saveOutputToLocalStorage(exportFilesEntity,
        new ExportRequest(), new ExportedRecordsListener(jobExecutionEntityRepository,
            1, jobExecutionEntity.getId()));
    assertEquals(1, exportStatistic.getExported());
    assertEquals(0, exportStatistic.getDuplicatedSrs());
    assertEquals(0, exportStatistic.getFailed());

    assertEquals(JobExecutionExportFilesStatus.ACTIVE, exportFilesEntity.getStatus());
    verify(jobExecutionEntityRepository, times(1))
        .save(isA(JobExecutionEntity.class));
    verify(localStorageWriter, times(1)).write(isA(String.class));
  }

  @SneakyThrows
  @Test
  void saveOutputToLocalStorageWhenLocalStorageCannotWriteTest() {
    var progress = new JobExecutionProgress();
    var jobExecution = JobExecution.builder().progress(progress).id(UUID.randomUUID()).build();
    var jobProfileEntity = new JobProfileEntity();
    jobProfileEntity.setId(UUID.randomUUID());
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setJobProfileId(jobProfileEntity.getId());

    var linkedDataResources = new ArrayList<LinkedDataResource>();
    var linkedDataResource = new LinkedDataResource();
    var exportId = UUID.randomUUID();
    linkedDataResource.setInventoryId(exportId.toString());
    linkedDataResource.setResource("{}}");
    linkedDataResources.add(linkedDataResource);
    ((LdTestExportStrategy) exportStrategy).setLinkedDataResources(linkedDataResources);

    JobExecutionExportFilesEntity exportFilesEntity = new JobExecutionExportFilesEntity()
        .withFileLocation("/tmp/" + jobExecution.getId().toString() + "/location")
        .withId(UUID.randomUUID()).withJobExecutionId(jobExecution.getId())
        .withFromId(UUID.randomUUID()).withToId(UUID.randomUUID())
        .withStatus(JobExecutionExportFilesStatus.ACTIVE);
    var exportIdEntity = new ExportIdEntity()
        .withJobExecutionId(exportFilesEntity.getJobExecutionId())
        .withId(0).withInstanceId(exportId);
    var slice = new SliceImpl<>(List.of(exportIdEntity), PageRequest.of(0, 1), false);

    when(exportIdEntityRepository.getExportIds(isA(UUID.class), isA(UUID.class), isA(UUID.class),
          isA(Pageable.class))).thenReturn(slice);
    var jobExecutionEntity = JobExecutionEntity.fromJobExecution(jobExecution);
    when(jobExecutionEntityRepository.getReferenceById(isA(UUID.class)))
        .thenReturn(jobExecutionEntity);
    when(exportIdEntityRepository.countExportIds(isA(UUID.class), isA(UUID.class),
        isA(UUID.class))).thenReturn(1L);
    var output = new ByteArrayOutputStream(2);
    output.write("{}".getBytes());
    when(linkedDataConverter.convertLdJsonToBibframe2Rdf(isA(String.class)))
        .thenReturn(output);
    doThrow(new LocalStorageWriterException("Cannot write")).when(localStorageWriter).close();

    var exportStatistic = exportStrategy.saveOutputToLocalStorage(exportFilesEntity,
        new ExportRequest(), new ExportedRecordsListener(jobExecutionEntityRepository,
            1, jobExecutionEntity.getId()));
    assertEquals(0, exportStatistic.getExported());
    assertEquals(0, exportStatistic.getDuplicatedSrs());
    assertEquals(1, exportStatistic.getFailed());

    assertEquals(JobExecutionExportFilesStatus.ACTIVE, exportFilesEntity.getStatus());
    verify(jobExecutionEntityRepository, times(1))
        .save(isA(JobExecutionEntity.class));
    verify(exportIdEntityRepository).countExportIds(exportFilesEntity.getJobExecutionId(),
        exportFilesEntity.getFromId(), exportFilesEntity.getToId());
  }

  @SneakyThrows
  @Test
  void saveOutputToLocalStorageWhenNoResults() {
    var progress = new JobExecutionProgress();
    var jobExecution = JobExecution.builder().progress(progress).id(UUID.randomUUID()).build();
    var jobProfileEntity = new JobProfileEntity();
    jobProfileEntity.setId(UUID.randomUUID());
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setJobProfileId(jobProfileEntity.getId());

    var linkedDataResources = new ArrayList<LinkedDataResource>();
    ((LdTestExportStrategy) exportStrategy).setLinkedDataResources(linkedDataResources);

    var exportId = UUID.randomUUID();
    JobExecutionExportFilesEntity exportFilesEntity = new JobExecutionExportFilesEntity()
        .withFileLocation("/tmp/" + jobExecution.getId().toString() + "/location")
        .withId(UUID.randomUUID()).withJobExecutionId(jobExecution.getId())
        .withFromId(UUID.randomUUID()).withToId(UUID.randomUUID())
        .withStatus(JobExecutionExportFilesStatus.ACTIVE);
    var exportIdEntity = new ExportIdEntity()
        .withJobExecutionId(exportFilesEntity.getJobExecutionId())
        .withId(0).withInstanceId(exportId);
    var slice = new SliceImpl<>(List.of(exportIdEntity), PageRequest.of(0, 1), false);

    when(exportIdEntityRepository.getExportIds(isA(UUID.class), isA(UUID.class), isA(UUID.class),
          isA(Pageable.class))).thenReturn(slice);
    var jobExecutionEntity = JobExecutionEntity.fromJobExecution(jobExecution);

    var exportStatistic = exportStrategy.saveOutputToLocalStorage(exportFilesEntity,
        new ExportRequest(), new ExportedRecordsListener(jobExecutionEntityRepository,
            1, jobExecutionEntity.getId()));
    assertEquals(0, exportStatistic.getExported());
    assertEquals(0, exportStatistic.getDuplicatedSrs());
    assertEquals(0, exportStatistic.getFailed());
    assertEquals(List.of(exportId), exportStatistic.getNotExistIds());

    assertEquals(JobExecutionExportFilesStatus.ACTIVE, exportFilesEntity.getStatus());
  }

  class LdTestExportStrategy extends AbstractLinkedDataExportStrategy {
    LdTestExportStrategy(int exportBatch) {
      super.setExportIdsBatch(exportBatch);
    }

    @Setter
    private List<LinkedDataResource> linkedDataResources = new ArrayList<>();

    @Override
    protected LocalStorageWriter createLocalStorageWriter(
        JobExecutionExportFilesEntity exportFilesEntity) {
      return localStorageWriter;
    }

    @Override
    List<LinkedDataResource> getLinkedDataResources(Set<UUID> externalIds) {
      return linkedDataResources;
    }
  }
}
