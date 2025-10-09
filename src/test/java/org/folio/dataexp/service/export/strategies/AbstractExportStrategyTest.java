package org.folio.dataexp.service.export.strategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.Setter;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionProgress;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.exception.export.LocalStorageWriterException;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.InstanceEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.service.JobExecutionService;
import org.folio.dataexp.service.export.LocalStorageWriter;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.util.ErrorCode;
import org.folio.s3.client.FolioS3Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

@ExtendWith(MockitoExtension.class)
class AbstractExportStrategyTest {

  @Mock
  private FolioS3Client s3Client;
  @Mock
  private JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  @Mock
  private ExportIdEntityRepository exportIdEntityRepository;
  @Mock
  private InstanceEntityRepository instanceEntityRepository;
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
  @Spy
  private JsonToMarcConverter jsonToMarcConverter;

  @InjectMocks
  private AbstractExportStrategy exportStrategy = new TestExportStrategy(1);

  @BeforeEach
  void clear() {
    ((TestExportStrategy) exportStrategy).setMarcRecords(new ArrayList<>());
    ((TestExportStrategy) exportStrategy)
        .setGeneratedMarcResult(new GeneratedMarcResult(UUID.randomUUID()));
  }

  @Test
  void saveOutputToLocalStorageTest() {
    var progress = new JobExecutionProgress();
    var jobExecution = JobExecution.builder().progress(progress).id(UUID.randomUUID()).build();
    var jobProfileEntity = new JobProfileEntity();
    jobProfileEntity.setId(UUID.randomUUID());
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setJobProfileId(jobProfileEntity.getId());

    var mappingProfileEntity = new MappingProfileEntity();
    mappingProfileEntity.setId(jobProfileEntity.getMappingProfileId());
    mappingProfileEntity.setMappingProfile(new MappingProfile());

    var exportId = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");
    var json = """
        {
            "leader": "00476cy  a22001574  4500"
        }""";
    var marcRecordEntity = new MarcRecordEntity(UUID.randomUUID(), exportId, json,
        "type", "ACTUAL", 'c', false, 0, false);
    var marcRecords = new ArrayList<MarcRecordEntity>();
    marcRecords.add(marcRecordEntity);
    marcRecords.add(marcRecordEntity);
    ((TestExportStrategy) exportStrategy).setMarcRecords(marcRecords);

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
    when(jobExecutionService.getById(exportIdEntity.getJobExecutionId()))
        .thenReturn(jobExecution);
    when(jobProfileEntityRepository.getReferenceById(jobProfileEntity.getId()))
        .thenReturn(jobProfileEntity);
    when(mappingProfileEntityRepository.getReferenceById(jobProfileEntity.getMappingProfileId()))
        .thenReturn(mappingProfileEntity);
    var jobExecutionEntity = JobExecutionEntity.fromJobExecution(jobExecution);
    when(jobExecutionEntityRepository.getReferenceById(isA(UUID.class)))
        .thenReturn(jobExecutionEntity);

    var exportStatistic = exportStrategy.saveOutputToLocalStorage(exportFilesEntity,
        new ExportRequest(), new ExportedMarcListener(jobExecutionEntityRepository,
            1, jobExecutionEntity.getId()));
    assertEquals(2, exportStatistic.getExported());
    assertEquals(1, exportStatistic.getDuplicatedSrs());
    assertEquals(0, exportStatistic.getFailed());

    assertEquals(JobExecutionExportFilesStatus.ACTIVE, exportFilesEntity.getStatus());

    verify(errorLogService, times(1))
        .saveGeneralErrorWithMessageValues(ErrorCode.ERROR_NON_EXISTING_INSTANCE.getCode(),
            List.of(marcRecordEntity.getId().toString()), jobExecution.getId());
    verify(errorLogService, times(1))
        .saveGeneralErrorWithMessageValues(eq(ErrorCode.ERROR_DUPLICATE_SRS_RECORD.getCode()),
            isA(List.class), eq(jobExecution.getId()));
    verify(jobExecutionEntityRepository, times(2))
        .save(isA(JobExecutionEntity.class));
    verify(localStorageWriter, times(2)).write(isA(String.class));
  }

  @Test
  void saveOutputToLocalStorageWhenMarcJsonInvalidTest() {
    var progress = new JobExecutionProgress();
    var jobExecution = JobExecution.builder().progress(progress).id(UUID.randomUUID()).build();
    var jobProfileEntity = new JobProfileEntity();
    jobProfileEntity.setId(UUID.randomUUID());
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setJobProfileId(jobProfileEntity.getId());

    var mappingProfileEntity = new MappingProfileEntity();
    mappingProfileEntity.setId(jobProfileEntity.getMappingProfileId());

    JobExecutionExportFilesEntity exportFilesEntity = new JobExecutionExportFilesEntity()
        .withFileLocation("/tmp/" + jobExecution.getId().toString() + "/location")
        .withId(UUID.randomUUID()).withJobExecutionId(jobExecution.getId())
        .withFromId(UUID.randomUUID()).withToId(UUID.randomUUID())
        .withStatus(JobExecutionExportFilesStatus.ACTIVE);

    var exportId = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");
    var exportIdEntity = new ExportIdEntity()
        .withJobExecutionId(exportFilesEntity.getJobExecutionId())
        .withId(0).withInstanceId(exportId);
    var json = """
        {
          invalid
        }""";
    var marcRecordEntity = new MarcRecordEntity(UUID.randomUUID(), exportId, json,
        "type", "ACTUAL", 'c', false, 0, false);
    var marcRecords = new ArrayList<MarcRecordEntity>();
    marcRecords.add(marcRecordEntity);
    ((TestExportStrategy) exportStrategy).setMarcRecords(marcRecords);

    var slice = new SliceImpl<>(List.of(exportIdEntity), PageRequest.of(0, 1), false);

    when(exportIdEntityRepository.getExportIds(isA(UUID.class), isA(UUID.class), isA(UUID.class),
        isA(Pageable.class))).thenReturn(slice);
    when(jobExecutionService.getById(exportIdEntity.getJobExecutionId()))
        .thenReturn(jobExecution);
    when(jobProfileEntityRepository.getReferenceById(jobProfileEntity.getId()))
        .thenReturn(jobProfileEntity);
    when(mappingProfileEntityRepository.getReferenceById(jobProfileEntity.getMappingProfileId()))
        .thenReturn(mappingProfileEntity);

    var jobExecutionEntity = JobExecutionEntity.fromJobExecution(jobExecution);
    var exportStatistic = exportStrategy.saveOutputToLocalStorage(exportFilesEntity,
        new ExportRequest(),
        new ExportedMarcListener(jobExecutionEntityRepository, 1, jobExecutionEntity.getId()));
    assertEquals(0, exportStatistic.getExported());
    assertEquals(0, exportStatistic.getDuplicatedSrs());
    assertEquals(1, exportStatistic.getFailed());

    assertEquals(JobExecutionExportFilesStatus.ACTIVE, exportFilesEntity.getStatus());

    verify(errorLogService).saveGeneralError(isA(String.class), eq(jobExecution.getId()));
  }

  @Test
  void saveOutputToLocalStorageWhenLocalStorageCanNotWriteTest() {
    var jobExecution = new JobExecution();
    var jobProfileEntity = new JobProfileEntity();
    jobProfileEntity.setId(UUID.randomUUID());
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setJobProfileId(jobProfileEntity.getId());
    var mappingProfileEntity = new MappingProfileEntity();
    mappingProfileEntity.setId(jobProfileEntity.getMappingProfileId());

    JobExecutionExportFilesEntity exportFilesEntity = new JobExecutionExportFilesEntity()
        .withFileLocation("/tmp/" + jobExecution.getId().toString() + "/location")
        .withId(UUID.randomUUID()).withJobExecutionId(jobExecution.getId())
        .withFromId(UUID.randomUUID()).withToId(UUID.randomUUID())
        .withStatus(JobExecutionExportFilesStatus.ACTIVE);

    var exportId = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");
    var exportIdEntity = new ExportIdEntity()
        .withJobExecutionId(exportFilesEntity.getJobExecutionId())
        .withId(0).withInstanceId(exportId);
    var json = """
        {
            "leader": "00476cy  a22001574  4500"
        }""";
    var marcRecordEntity = new MarcRecordEntity(UUID.randomUUID(), exportId, json,
        "type", "ACTUAL", 'c', false, 0, false);
    var marcRecords = new ArrayList<MarcRecordEntity>();
    marcRecords.add(marcRecordEntity);
    ((TestExportStrategy) exportStrategy).setMarcRecords(marcRecords);

    var slice = new SliceImpl<>(List.of(exportIdEntity), PageRequest.of(0, 1), false);

    when(exportIdEntityRepository.getExportIds(isA(UUID.class), isA(UUID.class), isA(UUID.class),
        isA(Pageable.class))).thenReturn(slice);
    when(jobExecutionService.getById(exportIdEntity.getJobExecutionId()))
        .thenReturn(jobExecution);
    when(jobProfileEntityRepository.getReferenceById(jobProfileEntity.getId()))
        .thenReturn(jobProfileEntity);
    when(mappingProfileEntityRepository.getReferenceById(jobProfileEntity.getMappingProfileId()))
        .thenReturn(mappingProfileEntity);
    when(exportIdEntityRepository.countExportIds(isA(UUID.class), isA(UUID.class),
        isA(UUID.class))).thenReturn(1L);
    doThrow(new LocalStorageWriterException("Can not write")).when(localStorageWriter).close();

    var exportStatistic = exportStrategy.saveOutputToLocalStorage(exportFilesEntity,
        new ExportRequest(), new ExportedMarcListener(null, 1000, null));
    assertEquals(0, exportStatistic.getExported());
    assertEquals(0, exportStatistic.getDuplicatedSrs());
    assertEquals(1, exportStatistic.getFailed());

    assertEquals(JobExecutionExportFilesStatus.ACTIVE, exportFilesEntity.getStatus());
    verify(exportIdEntityRepository).countExportIds(exportFilesEntity.getJobExecutionId(),
        exportFilesEntity.getFromId(), exportFilesEntity.getToId());
  }

  @Test
  void getAsJsonObjectTest() {
    var jsonAsString = "{'id':'123'}";
    var opt = exportStrategy.getAsJsonObject(jsonAsString);

    assertTrue(opt.isPresent());

    var jsonObject = opt.get();
    assertEquals("123", jsonObject.getAsString("id"));
  }

  class TestExportStrategy extends AbstractExportStrategy {

    TestExportStrategy(int exportBatch) {
      super.setExportIdsBatch(exportBatch);
    }

    @Setter
    private List<MarcRecordEntity> marcRecords = new ArrayList<>();
    @Setter
    private GeneratedMarcResult generatedMarcResult = new GeneratedMarcResult(UUID.randomUUID());

    @Override
    List<MarcRecordEntity> getMarcRecords(Set<UUID> externalIds, MappingProfile mappingProfile,
        ExportRequest exportRequest, UUID jobExecutionId) {
      return marcRecords;
    }

    @Override
    GeneratedMarcResult getGeneratedMarc(Set<UUID> ids, MappingProfile mappingProfile,
        ExportRequest exportRequest, UUID jobExecutionId,
        ExportStrategyStatistic exportStatistic) {
      return generatedMarcResult;
    }

    @Override
    Optional<ExportIdentifiersForDuplicateError> getIdentifiers(UUID id) {
      var identifiers = new ExportIdentifiersForDuplicateError();
      identifiers.setIdentifierHridMessage("hrid123");
      return Optional.of(identifiers);
    }

    @Override
    Map<UUID, MarcFields> getAdditionalMarcFieldsByExternalId(List<MarcRecordEntity> marcRecords,
        MappingProfile mappingProfile, UUID jobExecutionId) {
      return new HashMap<>();
    }

    @Override
    protected LocalStorageWriter createLocalStorageWrite(
        JobExecutionExportFilesEntity exportFilesEntity) {
      return localStorageWriter;
    }

    @Override
    public MarcRecordEntity getMarcRecord(UUID externalId) {
      throw new UnsupportedOperationException("The functionality is not required for testing.");
    }

    @Override
    public MappingProfile getDefaultMappingProfile() {
      throw new UnsupportedOperationException("The functionality is not required for testing.");
    }
  }
}
