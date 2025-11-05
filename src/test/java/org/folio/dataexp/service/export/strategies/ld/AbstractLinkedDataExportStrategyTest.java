package org.folio.dataexp.service.export.strategies.ld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import lombok.Builder;
import lombok.Setter;
import lombok.SneakyThrows;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.dto.LinkedDataResource;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.exception.export.LocalStorageWriterException;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.service.JobExecutionService;
import org.folio.dataexp.service.export.LocalStorageWriter;
import org.folio.dataexp.service.export.strategies.ExportedRecordsListener;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.s3.client.FolioS3Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
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
  private MappingProfileEntityRepository mappingProfileEntityRepository;
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
  private AbstractLinkedDataExportStrategy exportStrategy = new LdTestExportStrategy(1, 5);

  @BeforeEach
  void clear() {
    ((LdTestExportStrategy) exportStrategy).setLinkedDataResources(new ArrayList<>());
  }

  @Builder
  private static class TestPreparation {
    UUID exportId;
    JobExecutionExportFilesEntity exportFilesEntity;
    JobExecutionEntity jobExecutionEntity;
  }

  private List<LinkedDataResource> generateLinkedData(int count) {
    var linkedDataResources = new ArrayList<LinkedDataResource>();
    IntStream.range(0, count).forEach(index -> {
      var exportId = UUID.randomUUID();
      var resource = new LinkedDataResource();
      resource.setInventoryId(exportId.toString());
      resource.setResource("{}");
      linkedDataResources.add(resource);
    });
    return linkedDataResources;
  }

  private TestPreparation prepare(int count, boolean include, boolean updateJob) {
    var jobProfileEntity = new JobProfileEntity();
    var jobExecution = JobExecution.builder()
            .progress(new JobExecutionProgress())
            .id(UUID.randomUUID())
            .build();
    jobProfileEntity.setId(UUID.randomUUID());
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setJobProfileId(jobProfileEntity.getId());

    var mappingProfileEntity = new MappingProfileEntity();
    mappingProfileEntity.setId(jobProfileEntity.getMappingProfileId());
    mappingProfileEntity.setMappingProfile(new MappingProfile());

    var linkedDataResources = generateLinkedData(count);
    if (include) {
      ((LdTestExportStrategy) exportStrategy)
          .setLinkedDataResources(linkedDataResources);
    } else {
      ((LdTestExportStrategy) exportStrategy)
          .setLinkedDataResources(new ArrayList<LinkedDataResource>());
    }

    var fromId = UUID.randomUUID();
    var toId = UUID.randomUUID();
    JobExecutionExportFilesEntity exportFilesEntity = new JobExecutionExportFilesEntity()
        .withFileLocation("/tmp/" + jobExecution.getId().toString() + "/location")
        .withId(UUID.randomUUID()).withJobExecutionId(jobExecution.getId())
        .withFromId(fromId).withToId(toId)
        .withStatus(JobExecutionExportFilesStatus.ACTIVE);
    var resourceIndex = new AtomicInteger(0);
    linkedDataResources.stream()
        .map(LinkedDataResource::getInventoryId)
        .map(id -> {
          return new ExportIdEntity()
              .withJobExecutionId(exportFilesEntity.getJobExecutionId())
              .withId(resourceIndex.get())
              .withInstanceId(UUID.fromString(id));
        })
        .map(entity -> {
          return new SliceImpl<>(
              List.of(entity),
              PageRequest.of(resourceIndex.get(), 1),
              resourceIndex.get() + 1 != linkedDataResources.size()
          );
        })
        .forEach(slice -> {
          when(exportIdEntityRepository.getExportIds(jobExecution.getId(), fromId, toId,
              PageRequest.of(resourceIndex.getAndIncrement(), 1))).thenReturn(slice);
        });

    when(jobExecutionService.getById(exportFilesEntity.getJobExecutionId()))
        .thenReturn(jobExecution);
    when(jobProfileEntityRepository.getReferenceById(jobProfileEntity.getId()))
        .thenReturn(jobProfileEntity);
    when(mappingProfileEntityRepository.getReferenceById(jobProfileEntity.getMappingProfileId()))
        .thenReturn(mappingProfileEntity);
    var jobExecutionEntity = JobExecutionEntity.fromJobExecution(jobExecution);
    if (include && updateJob) {
      when(jobExecutionEntityRepository.getReferenceById(isA(UUID.class)))
          .thenReturn(jobExecutionEntity);
    }

    UUID exportId = null;
    if (count == 1) {
      exportId = UUID.fromString(linkedDataResources.get(0).getInventoryId());
    }

    return TestPreparation.builder()
        .exportId(exportId)
        .exportFilesEntity(exportFilesEntity)
        .jobExecutionEntity(jobExecutionEntity)
        .build();
  }

  @SneakyThrows
  @Test
  void saveOutputToLocalStorageWhenLocalStorageCannotWriteTest() {
    var output = new ByteArrayOutputStream(2);
    output.write("{}".getBytes());

    when(exportIdEntityRepository.countExportIds(isA(UUID.class), isA(UUID.class),
        isA(UUID.class))).thenReturn(1L);
    when(linkedDataConverter.convertLdJsonToBibframe2Rdf(isA(String.class)))
        .thenReturn(output);
    doThrow(new LocalStorageWriterException("Cannot write")).when(localStorageWriter).close();

    var preparation = prepare(1, true, true);
    var exportStatistic = exportStrategy.saveOutputToLocalStorage(preparation.exportFilesEntity,
        new ExportRequest(), new ExportedRecordsListener(jobExecutionEntityRepository,
            1, preparation.jobExecutionEntity.getId()));

    assertEquals(0, exportStatistic.getExported());
    assertEquals(0, exportStatistic.getDuplicatedSrs());
    assertEquals(1, exportStatistic.getFailed());
    assertEquals(JobExecutionExportFilesStatus.ACTIVE, preparation.exportFilesEntity.getStatus());
    verify(jobExecutionEntityRepository, times(1))
        .save(isA(JobExecutionEntity.class));
    verify(exportIdEntityRepository).countExportIds(
        preparation.exportFilesEntity.getJobExecutionId(),
        preparation.exportFilesEntity.getFromId(),
        preparation.exportFilesEntity.getToId());
  }

  @SneakyThrows
  @Test
  void saveOutputToLocalStorageWhenNoResults() {
    var preparation = prepare(1, false, false);
    var exportStatistic = exportStrategy.saveOutputToLocalStorage(preparation.exportFilesEntity,
        new ExportRequest(), new ExportedRecordsListener(jobExecutionEntityRepository,
            1, preparation.jobExecutionEntity.getId()));

    assertEquals(0, exportStatistic.getExported());
    assertEquals(0, exportStatistic.getDuplicatedSrs());
    assertEquals(0, exportStatistic.getFailed());
    assertEquals(List.of(preparation.exportId), exportStatistic.getNotExistIds());
    assertEquals(JobExecutionExportFilesStatus.ACTIVE, preparation.exportFilesEntity.getStatus());
    verify(localStorageWriter, never()).write(isA(String.class));
  }

  @SneakyThrows
  @Test
  void saveOutputToLocalStorageConvertErrorTest() {
    doThrow(JsonProcessingException.class).when(linkedDataConverter)
        .convertLdJsonToBibframe2Rdf(isA(String.class));

    var preparation = prepare(1, true, false);
    var exportStatistic = exportStrategy.saveOutputToLocalStorage(preparation.exportFilesEntity,
        new ExportRequest(), new ExportedRecordsListener(jobExecutionEntityRepository,
            1, preparation.jobExecutionEntity.getId()));

    assertEquals(0, exportStatistic.getExported());
    assertEquals(0, exportStatistic.getDuplicatedSrs());
    assertEquals(1, exportStatistic.getFailed());
    assertEquals(JobExecutionExportFilesStatus.ACTIVE, preparation.exportFilesEntity.getStatus());
    verify(errorLogService, times(1))
        .saveGeneralError(isA(String.class), isA(UUID.class));
    verify(jobExecutionEntityRepository, never())
        .save(isA(JobExecutionEntity.class));
    verify(localStorageWriter, never()).write(isA(String.class));
  }

  // Simulate a batch size of one with one batch handled per thread
  @SneakyThrows
  @ParameterizedTest
  @ValueSource(ints = { 1, 2, 3, 10, 15, 100 })
  void saveOutputToLocalStorageMultithreaded(int threads) {
    var output = new ByteArrayOutputStream(2);
    output.write("{}".getBytes());
    when(linkedDataConverter.convertLdJsonToBibframe2Rdf(isA(String.class)))
        .thenReturn(output);
    when(localStorageWriter.getReader())
        .thenReturn(Optional.of(new BufferedReader(new StringReader("{}"))));

    var preparation = prepare(threads, true, true);
    var exportStatistic = exportStrategy.saveOutputToLocalStorage(preparation.exportFilesEntity,
        new ExportRequest(), new ExportedRecordsListener(jobExecutionEntityRepository,
            1, preparation.jobExecutionEntity.getId()));
    assertEquals(threads, exportStatistic.getExported());
    assertEquals(0, exportStatistic.getDuplicatedSrs());
    assertEquals(0, exportStatistic.getFailed());

    assertEquals(JobExecutionExportFilesStatus.ACTIVE, preparation.exportFilesEntity.getStatus());
    verify(jobExecutionEntityRepository, times(threads))
        .save(isA(JobExecutionEntity.class));
    verify(localStorageWriter, times(threads + 1)).write(isA(String.class));
  }

  class LdTestExportStrategy extends AbstractLinkedDataExportStrategy {
    LdTestExportStrategy(int exportBatch, int threadPool) {
      super.setExportIdsBatch(exportBatch);
      super.setProcessSlicesThreadPoolSize(threadPool);
    }

    @Setter
    private List<LinkedDataResource> linkedDataResources = new ArrayList<>();

    @Override
    protected LocalStorageWriter createLocalStorageWriter(
        JobExecutionExportFilesEntity exportFilesEntity,
        Integer pageNumber) {
      return localStorageWriter;
    }

    @Override
    List<LinkedDataResource> getLinkedDataResources(Set<UUID> externalIds) {
      return this.linkedDataResources.stream()
        .filter(resource -> externalIds.contains(UUID.fromString(resource.getInventoryId())))
        .toList();
    }
  }
}
