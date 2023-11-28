package org.folio.dataexp.service.export.strategies;

import lombok.Setter;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesStatus;
import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.s3.client.FolioS3Client;
import org.folio.s3.client.RemoteStorageWriter;
import org.folio.s3.exception.S3ClientException;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractExportStrategyTest {

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
  private RemoteStorageWriter remoteStorageWriter;
  @Spy
  private JsonToMarcConverter jsonToMarcConverter;

  @InjectMocks
  private AbstractExportStrategy exportStrategy = new TestExportStrategy(1);

  @BeforeEach
  void clear(){
    ((TestExportStrategy)exportStrategy).setMarcRecords(new ArrayList<>());
    ((TestExportStrategy)exportStrategy).setGeneratedMarcResult(new GeneratedMarcResult());
  }

  @Test
  void saveMarcToRemoteStorageTest() {
    var exportId = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");
    var jobExecutionEntity = new JobExecutionEntity();
    var jobProfileEntity = new JobProfileEntity();
    jobProfileEntity.setId(UUID.randomUUID());
    jobExecutionEntity.setId(UUID.randomUUID());
    jobExecutionEntity.setJobProfileId(jobProfileEntity.getId());
    var mappingProfileEntity = new MappingProfileEntity();
    mappingProfileEntity.setId(jobProfileEntity.getMappingProfileId());

    JobExecutionExportFilesEntity exportFilesEntity = new JobExecutionExportFilesEntity()
      .withFileLocation("/tmp/" + jobExecutionEntity.getId().toString() + "/location").withId(UUID.randomUUID()).withJobExecutionId(jobExecutionEntity.getId())
      .withFromId(UUID.randomUUID()).withToId(UUID.randomUUID()).withStatus(JobExecutionExportFilesStatus.COMPLETED);

    var exportIdEntity = new ExportIdEntity().withJobExecutionId(exportFilesEntity.getJobExecutionId())
      .withId(0).withInstanceId(exportId);
        var json = """
      {
          "leader": "00476cy  a22001574  4500"
      }""";
    var marcRecordEntity = new MarcRecordEntity(UUID.randomUUID(), exportId, json);
    var marcRecords = new ArrayList<MarcRecordEntity>();
    marcRecords.add(marcRecordEntity);
    marcRecords.add(marcRecordEntity);
    ((TestExportStrategy)exportStrategy).setMarcRecords(marcRecords);

    var slice = new SliceImpl<>(List.of(exportIdEntity), PageRequest.of(0, 1), false);

    when(exportIdEntityRepository.getExportIds(isA(UUID.class), isA(UUID.class), isA(UUID.class), isA(Pageable.class))).thenReturn(slice);
    when(jobExecutionEntityRepository.getReferenceById(exportIdEntity.getJobExecutionId())).thenReturn(jobExecutionEntity);
    when(jobProfileEntityRepository.getReferenceById(jobProfileEntity.getId())).thenReturn(jobProfileEntity);
    when(mappingProfileEntityRepository.getReferenceById(jobProfileEntity.getMappingProfileId())).thenReturn(mappingProfileEntity);

    var exportStatistic = exportStrategy.saveMarcToRemoteStorage(exportFilesEntity);
    assertEquals(2, exportStatistic.getExported());
    assertEquals(1, exportStatistic.getDuplicatedSrs());
    assertEquals(0, exportStatistic.getFailed());

    assertEquals(JobExecutionExportFilesStatus.COMPLETED, exportFilesEntity.getStatus());

    verify(remoteStorageWriter, times(2)).write(isA(String.class));
  }

  @Test
  void saveMarcToRemoteStorageWhenRemoteStorageCanNotWriteTest() {
    var exportId = UUID.fromString("0eaa7eef-9633-4c7e-af09-796315ebc576");
    var jobExecutionEntity = new JobExecutionEntity();
    var jobProfileEntity = new JobProfileEntity();
    jobProfileEntity.setId(UUID.randomUUID());
    jobExecutionEntity.setId(UUID.randomUUID());
    jobExecutionEntity.setJobProfileId(jobProfileEntity.getId());
    var mappingProfileEntity = new MappingProfileEntity();
    mappingProfileEntity.setId(jobProfileEntity.getMappingProfileId());

    JobExecutionExportFilesEntity exportFilesEntity = new JobExecutionExportFilesEntity()
      .withFileLocation("/tmp/" + jobExecutionEntity.getId().toString() + "/location").withId(UUID.randomUUID()).withJobExecutionId(jobExecutionEntity.getId())
      .withFromId(UUID.randomUUID()).withToId(UUID.randomUUID()).withStatus(JobExecutionExportFilesStatus.COMPLETED);

    var exportIdEntity = new ExportIdEntity().withJobExecutionId(exportFilesEntity.getJobExecutionId())
      .withId(0).withInstanceId(exportId);
    var json = """
      {
          "leader": "00476cy  a22001574  4500"
      }""";
    var marcRecordEntity = new MarcRecordEntity(UUID.randomUUID(), exportId, json);
    var marcRecords = new ArrayList<MarcRecordEntity>();
    marcRecords.add(marcRecordEntity);
    ((TestExportStrategy)exportStrategy).setMarcRecords(marcRecords);

    var slice = new SliceImpl<>(List.of(exportIdEntity), PageRequest.of(0, 1), false);

    when(exportIdEntityRepository.getExportIds(isA(UUID.class), isA(UUID.class), isA(UUID.class), isA(Pageable.class))).thenReturn(slice);
    when(jobExecutionEntityRepository.getReferenceById(exportIdEntity.getJobExecutionId())).thenReturn(jobExecutionEntity);
    when(jobProfileEntityRepository.getReferenceById(jobProfileEntity.getId())).thenReturn(jobProfileEntity);
    when(mappingProfileEntityRepository.getReferenceById(jobProfileEntity.getMappingProfileId())).thenReturn(mappingProfileEntity);
    when(exportIdEntityRepository.countExportIds(isA(UUID.class), isA(UUID.class), isA(UUID.class))).thenReturn(1L);
    doThrow(new S3ClientException("Can not write")).when(remoteStorageWriter).close();

    var exportStatistic = exportStrategy.saveMarcToRemoteStorage(exportFilesEntity);
    assertEquals(0, exportStatistic.getExported());
    assertEquals(0, exportStatistic.getDuplicatedSrs());
    assertEquals(1, exportStatistic.getFailed());

    assertEquals(JobExecutionExportFilesStatus.FAILED, exportFilesEntity.getStatus());
    verify(exportIdEntityRepository).countExportIds(exportFilesEntity.getJobExecutionId(), exportFilesEntity.getFromId(), exportFilesEntity.getToId());
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
    private GeneratedMarcResult generatedMarcResult = new GeneratedMarcResult();

    @Override
    List<MarcRecordEntity> getMarcRecords(Set<UUID> externalIds) {
      return marcRecords;
    }

    @Override
    GeneratedMarcResult getGeneratedMarc(Set<UUID> ids, MappingProfile mappingProfile) {
      return generatedMarcResult;
    }

    @Override
    protected RemoteStorageWriter createRemoteStorageWrite(JobExecutionExportFilesEntity exportFilesEntity) {
      return remoteStorageWriter;
    }
  }
}
