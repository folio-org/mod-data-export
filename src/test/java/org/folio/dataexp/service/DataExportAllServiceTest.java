package org.folio.dataexp.service;

import lombok.SneakyThrows;
import com.github.jknack.handlebars.internal.Files;
import org.codehaus.plexus.util.StringUtils;
import org.folio.dataexp.domain.dto.ExportAllRequest;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobProfile;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.folio.dataexp.util.S3FilePathUtils.getPathToStoredFiles;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DataExportAllServiceTest extends ServiceInitializer {

  @Autowired
  private DataExportAllService dataExportAllService;
  @Autowired
  private MappingProfileEntityRepository mappingProfileEntityRepository;
  @Autowired
  private JobProfileEntityRepository jobProfileEntityRepository;
  @Autowired
  private FileDefinitionEntityRepository fileDefinitionEntityRepository;

  private static final UUID CUSTOM_INSTANCE_MAPPING_PROFILE_ID = UUID.randomUUID();
  private static final UUID CUSTOM_HOLDINGS_MAPPING_PROFILE_ID = UUID.randomUUID();
  private static final UUID CUSTOM_INSTANCE_JOB_PROFILE_ID = UUID.randomUUID();
  private static final UUID CUSTOM_HOLDINGS_JOB_PROFILE_ID = UUID.randomUUID();

  @SneakyThrows
  @Test
  void exportAllInstancesNotSuppressedNoErrorsTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();
      var exportAllRequest = new ExportAllRequest();
      dataExportAllService.postDataExportAll(exportAllRequest);

      await().atMost(4, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        var fileDefinition = fileDefinitionEntityRepository.getFileDefinitionByJobExecutionId(jobExecution.getId().toString()).get(0).getFileDefinition();
        var expectedFileName = "instance-all.csv";
        assertEquals(expectedFileName, fileDefinition.getFileName());

        assertEquals(13, jobExecution.getJobExecution().getProgress().getTotal());

        var fileToExport  =  String.format("%s-%s.mrc", "instance-all", jobExecution.getJobExecution().getHrId());
        var s3path = getPathToStoredFiles(jobExecution.getId(), fileToExport);
        String outputMrcFile = Files.read(s3Client.read(s3path), Charset.defaultCharset());

        // Check FOLIO
        assertThat(outputMrcFile).containsOnlyOnce("i011e1aea-222d-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("i011e1aea-111d-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("i1640f178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i1880f178-f243-4e4a-bf1c-9e1e62b3171d");

        // Check MARC
        assertThat(outputMrcFile).containsOnlyOnce("s8888893e-f9e2-4cb2-a52b-e9155acfc119");
        assertThat(outputMrcFile).containsOnlyOnce("s555d1aea-222d-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s66661aea-222d-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s77771aea-222d-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s88881aea-222d-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s10001aea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s20002aea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s30003aea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s7272723e-f9e2-4cb2-a52b-e9155acfc119");

        assertThat(StringUtils.countMatches(outputMrcFile, "999")).isEqualTo(13);
      });
    }
  }


  @SneakyThrows
  @Test
  void exportAllInstancesNotSuppressedNoErrorsCustomInstanceProfileTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();

      createCustomInstanceJobProfile();

      var exportAllRequest = new ExportAllRequest().jobProfileId(CUSTOM_INSTANCE_JOB_PROFILE_ID);
      dataExportAllService.postDataExportAll(exportAllRequest);

      await().atMost(2, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        assertEquals(13, jobExecution.getJobExecution().getProgress().getTotal());

        var fileToExport  =  String.format("%s-%s.mrc", "instance-all", jobExecution.getJobExecution().getHrId());
        var s3path = getPathToStoredFiles(jobExecution.getId(), fileToExport);
        String outputMrcFile = Files.read(s3Client.read(s3path), Charset.defaultCharset());

        // Check FOLIO
        assertThat(outputMrcFile).containsOnlyOnce("i011e1aea-222d-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("i011e1aea-111d-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("i1640f178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i1880f178-f243-4e4a-bf1c-9e1e62b3171d");

        // Check MARC
        assertThat(outputMrcFile).containsOnlyOnce("i2320f178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i4444f178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i6666f178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i7777f178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i8888f178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i10001178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i20002178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i30003178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i72727277-f243-4e4a-bf1c-9e1e62b3171d");

        assertThat(StringUtils.countMatches(outputMrcFile, "999")).isEqualTo(13);
      });
    }
  }

  @SneakyThrows
  @Test
  void exportAllInstancesNoErrorsTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();
      var exportAllRequest = new ExportAllRequest().suppressedFromDiscovery(true);
      dataExportAllService.postDataExportAll(exportAllRequest);

      await().atMost(4, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        assertEquals(22, jobExecution.getJobExecution().getProgress().getTotal());

        var fileToExport = String.format("%s-%s.mrc", "instance-all", jobExecution.getJobExecution().getHrId());
        var s3path = getPathToStoredFiles(jobExecution.getId(), fileToExport);
        String outputMrcFile = Files.read(s3Client.read(s3path), Charset.defaultCharset());

        // Check FOLIO
        // Suppressed:
        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i1770f178-f243-4e4a-bf1c-9e1e62b3171d");

        // Non-deleted: (nothing)

        // Not suppressed:
        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i1640f178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i1880f178-f243-4e4a-bf1c-9e1e62b3171d");

        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i011e1aea-111d-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("i011e1aea-222d-4d1d-957d-0abcdd0e9acd");

        // Check MARC
        // Suppressed:
        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("s40004aea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s50005aea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s60006aea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s66661aea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s77771aea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s88881aea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s7777793e-f9e2-4cb2-a52b-e9155acfc119");

        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("s7171713e-f9e2-4cb2-a52b-e9155acfc119");

        // Not suppressed:
        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("s10001aea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s20002aea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s30003aea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s66661aea-222d-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s77771aea-222d-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s88881aea-222d-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s555d1aea-222d-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s8888893e-f9e2-4cb2-a52b-e9155acfc119");

        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("s7272723e-f9e2-4cb2-a52b-e9155acfc119");

        assertThat(StringUtils.countMatches(outputMrcFile, "999")).isEqualTo(22);
      });
    }
  }

  @SneakyThrows
  @Test
  void exportAllInstancesNoErrorsCustomInstanceProfileTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();

      createCustomInstanceJobProfile();

      var exportAllRequest = new ExportAllRequest().suppressedFromDiscovery(true).jobProfileId(CUSTOM_INSTANCE_JOB_PROFILE_ID);
      dataExportAllService.postDataExportAll(exportAllRequest);

      await().atMost(4, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        assertEquals(22, jobExecution.getJobExecution().getProgress().getTotal());

        var fileToExport = String.format("%s-%s.mrc", "instance-all", jobExecution.getJobExecution().getHrId());
        var s3path = getPathToStoredFiles(jobExecution.getId(), fileToExport);
        String outputMrcFile = Files.read(s3Client.read(s3path), Charset.defaultCharset());

        // Check FOLIO
        // Suppressed:
        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i1770f178-f243-4e4a-bf1c-9e1e62b3171d");

        // Non-deleted: (nothing)

        // Not suppressed:
        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i1640f178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i1880f178-f243-4e4a-bf1c-9e1e62b3171d");

        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i011e1aea-111d-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("i011e1aea-222d-4d1d-957d-0abcdd0e9acd");

        // Check MARC
        // Suppressed:
        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i40004178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i50005178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i60006178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i6666f178-1111-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i7777f178-1111-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i8888f178-1111-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i1640f777-f243-4e4a-bf1c-9e1e62b3171d");

        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i71717177-f243-4e4a-bf1c-9e1e62b3171d");

        // Not suppressed:
        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i10001178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i20002178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i30003178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i6666f178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i7777f178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i8888f178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i4444f178-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i2320f178-f243-4e4a-bf1c-9e1e62b3171d");

        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i72727277-f243-4e4a-bf1c-9e1e62b3171d");

        assertThat(StringUtils.countMatches(outputMrcFile, "999")).isEqualTo(22);

        removeCustomInstanceJobProfile();
      });
    }
  }

  @SneakyThrows
  @Test
  void exportAllInstancesNonDeletedNoErrorsTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();
      var exportAllRequest = new ExportAllRequest().suppressedFromDiscovery(true).deletedRecords(false);
      dataExportAllService.postDataExportAll(exportAllRequest);

      await().atMost(4, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        assertEquals(4, jobExecution.getJobExecution().getProgress().getTotal());

        var fileToExport = String.format("%s-%s.mrc", "instance-all", jobExecution.getJobExecution().getHrId());
        var s3path = getPathToStoredFiles(jobExecution.getId(), fileToExport);
        String outputMrcFile = Files.read(s3Client.read(s3path), Charset.defaultCharset());

        // Check FOLIO
        // Suppressed:
        // Non-deleted: (nothing)

        // Not suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i011e1aea-111d-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("i011e1aea-222d-4d1d-957d-0abcdd0e9acd");

        // Check MARC
        // Suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("s7171713e-f9e2-4cb2-a52b-e9155acfc119");

        // Not suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("s7272723e-f9e2-4cb2-a52b-e9155acfc119");

        assertThat(StringUtils.countMatches(outputMrcFile, "999")).isEqualTo(4);
      });
    }
  }

  @SneakyThrows
  @Test
  void exportAllInstancesNonDeletedNoErrorsCustomInstanceProfileTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();

      createCustomInstanceJobProfile();

      var exportAllRequest = new ExportAllRequest().suppressedFromDiscovery(true).deletedRecords(false)
          .jobProfileId(CUSTOM_INSTANCE_JOB_PROFILE_ID);
      dataExportAllService.postDataExportAll(exportAllRequest);

      await().atMost(4, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        assertEquals(4, jobExecution.getJobExecution().getProgress().getTotal());

        var fileToExport = String.format("%s-%s.mrc", "instance-all", jobExecution.getJobExecution().getHrId());
        var s3path = getPathToStoredFiles(jobExecution.getId(), fileToExport);
        String outputMrcFile = Files.read(s3Client.read(s3path), Charset.defaultCharset());

        // Check FOLIO
        // Suppressed:
        // Non-deleted: (nothing)

        // Not suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i011e1aea-111d-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("i011e1aea-222d-4d1d-957d-0abcdd0e9acd");

        // Check MARC
        // Suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i71717177-f243-4e4a-bf1c-9e1e62b3171d");

        // Not suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i72727277-f243-4e4a-bf1c-9e1e62b3171d");

        assertThat(StringUtils.countMatches(outputMrcFile, "999")).isEqualTo(4);

        removeCustomInstanceJobProfile();
      });
    }
  }

  @SneakyThrows
  @Test
  void exportAllInstancesNonSuppressedNonDeletedNoErrorsTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();
      var exportAllRequest = new ExportAllRequest().suppressedFromDiscovery(false).deletedRecords(false);
      dataExportAllService.postDataExportAll(exportAllRequest);

      await().atMost(4, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        assertEquals(3, jobExecution.getJobExecution().getProgress().getTotal());

        var fileToExport = String.format("%s-%s.mrc", "instance-all", jobExecution.getJobExecution().getHrId());
        var s3path = getPathToStoredFiles(jobExecution.getId(), fileToExport);
        String outputMrcFile = Files.read(s3Client.read(s3path), Charset.defaultCharset());

        // Check FOLIO
        // Not suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i011e1aea-111d-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("i011e1aea-222d-4d1d-957d-0abcdd0e9acd");

        // Check MARC
        // Not suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("s7272723e-f9e2-4cb2-a52b-e9155acfc119");

        assertThat(StringUtils.countMatches(outputMrcFile, "999")).isEqualTo(3);
      });
    }
  }

  @SneakyThrows
  @Test
  void exportAllInstancesNonSuppressedNonDeletedNoErrorsCustomInstanceProfileTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();

      createCustomInstanceJobProfile();

      var exportAllRequest = new ExportAllRequest().suppressedFromDiscovery(false).deletedRecords(false)
          .jobProfileId(CUSTOM_INSTANCE_JOB_PROFILE_ID);
      dataExportAllService.postDataExportAll(exportAllRequest);

      await().atMost(4, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        assertEquals(3, jobExecution.getJobExecution().getProgress().getTotal());
        var fileToExport = String.format("%s-%s.mrc", "instance-all", jobExecution.getJobExecution().getHrId());
        var s3path = getPathToStoredFiles(jobExecution.getId(), fileToExport);
        String outputMrcFile = Files.read(s3Client.read(s3path), Charset.defaultCharset());

        // Check FOLIO
        // Not suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i011e1aea-111d-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("i011e1aea-222d-4d1d-957d-0abcdd0e9acd");

        // Check MARC
        // Not suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i72727277-f243-4e4a-bf1c-9e1e62b3171d");

        assertThat(StringUtils.countMatches(outputMrcFile, "999")).isEqualTo(3);

        removeCustomInstanceJobProfile();
      });
    }
  }

  @SneakyThrows
  @Test
  void exportAllHoldingsNoErrorsTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();
      var exportAllRequest = new ExportAllRequest().idType(ExportAllRequest.IdTypeEnum.HOLDING)
        .jobProfileId(DEFAULT_HOLDINGS_JOB_PROFILE).suppressedFromDiscovery(true);
      dataExportAllService.postDataExportAll(exportAllRequest);

      await().atMost(4, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        assertEquals(18, jobExecution.getJobExecution().getProgress().getTotal());

        var fileDefinition = fileDefinitionEntityRepository.getFileDefinitionByJobExecutionId(jobExecution.getId().toString()).get(0).getFileDefinition();
        var expectedFileName = "holding-all.csv";
        assertEquals(expectedFileName, fileDefinition.getFileName());

        var fileToExport = String.format("%s-%s.mrc", "holding-all", jobExecution.getJobExecution().getHrId());
        var s3path = getPathToStoredFiles(jobExecution.getId(), fileToExport);
        String outputMrcFile = Files.read(s3Client.read(s3path), Charset.defaultCharset());

        // Check FOLIO
        // Suppressed:
        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i1033f777-f243-4e4a-bf1c-9e1e62b3171d");

        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i1033bb50-7c9b-48b0-86eb-178a494e25fe");

        // Not suppressed:
        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i1089f777-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i1798f777-f243-4e4a-bf1c-9e1e62b3171d");

        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i0c45bb50-7c9b-48b0-86eb-178a494e25fe");
        assertThat(outputMrcFile).containsOnlyOnce("i1f45bb50-7c9b-48b0-86eb-178a494e25fe");

        // Check MARC
        // Suppressed:
        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("s444444ea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s777777ea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s888888ea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s15471150-7c9b-48b0-86eb-178a494e25fe");

        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("s111111ea-1111-4d1d-957d-0abcdd0e9acd");

        // Not suppressed:
        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("s555555ea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s666666ea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s912349ea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s25472250-7c9b-48b0-86eb-178a494e25fe");
        assertThat(outputMrcFile).containsOnlyOnce("s35473350-7c9b-48b0-86eb-178a494e25fe");

        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("s222222ea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s333333ea-1111-4d1d-957d-0abcdd0e9acd");

        assertThat(StringUtils.countMatches(outputMrcFile, "999")).isEqualTo(18);
      });
    }
  }

  @SneakyThrows
  @Test
  void exportAllHoldingsNoErrorsCustomHoldingsProfileTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();

      createCustomHoldingsJobProfile();

      var exportAllRequest = new ExportAllRequest().idType(ExportAllRequest.IdTypeEnum.HOLDING)
          .jobProfileId(CUSTOM_HOLDINGS_JOB_PROFILE_ID).suppressedFromDiscovery(true);
      dataExportAllService.postDataExportAll(exportAllRequest);

      await().atMost(4, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        assertEquals(18, jobExecution.getJobExecution().getProgress().getTotal());

        var fileToExport = String.format("%s-%s.mrc", "holding-all", jobExecution.getJobExecution().getHrId());
        var s3path = getPathToStoredFiles(jobExecution.getId(), fileToExport);
        String outputMrcFile = Files.read(s3Client.read(s3path), Charset.defaultCharset());

        // Check FOLIO
        // Suppressed:
        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i1033f777-f243-4e4a-bf1c-9e1e62b3171d");

        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i1033bb50-7c9b-48b0-86eb-178a494e25fe");

        // Not suppressed:
        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i1089f777-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i1798f777-f243-4e4a-bf1c-9e1e62b3171d");

        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i0c45bb50-7c9b-48b0-86eb-178a494e25fe");
        assertThat(outputMrcFile).containsOnlyOnce("i1f45bb50-7c9b-48b0-86eb-178a494e25fe");

        // Check MARC
        // Suppressed:
        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i44444477-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i77777777-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i88888877-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i15461150-7c9b-48b0-86eb-178a494e25fe");

        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i11111150-7c9b-48b0-86eb-178a494e25fe");

        // Not suppressed:
        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i55555577-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i66666677-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i91234977-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i25462250-7c9b-48b0-86eb-178a494e25fe");
        assertThat(outputMrcFile).containsOnlyOnce("i35463350-7c9b-48b0-86eb-178a494e25fe");

        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i22222250-7c9b-48b0-86eb-178a494e25fe");
        assertThat(outputMrcFile).containsOnlyOnce("i33333350-7c9b-48b0-86eb-178a494e25fe");

        assertThat(StringUtils.countMatches(outputMrcFile, "999")).isEqualTo(18);

        removeCustomHoldingsJobProfile();
      });
    }
  }

  @SneakyThrows
  @Test
  void exportAllHoldingsNonSuppressedNoErrorsTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();
      var exportAllRequest = new ExportAllRequest().idType(ExportAllRequest.IdTypeEnum.HOLDING)
          .jobProfileId(DEFAULT_HOLDINGS_JOB_PROFILE);
      dataExportAllService.postDataExportAll(exportAllRequest);

      await().atMost(4, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        assertEquals(11, jobExecution.getJobExecution().getProgress().getTotal());

        var fileToExport = String.format("%s-%s.mrc", "holding-all", jobExecution.getJobExecution().getHrId());
        var s3path = getPathToStoredFiles(jobExecution.getId(), fileToExport);
        String outputMrcFile = Files.read(s3Client.read(s3path), Charset.defaultCharset());

        // Check FOLIO
        // Not suppressed:
        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i1089f777-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i1798f777-f243-4e4a-bf1c-9e1e62b3171d");

        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i0c45bb50-7c9b-48b0-86eb-178a494e25fe");
        assertThat(outputMrcFile).containsOnlyOnce("i1f45bb50-7c9b-48b0-86eb-178a494e25fe");

        // Check MARC
        // Not suppressed:
        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("s555555ea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s666666ea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s912349ea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s25472250-7c9b-48b0-86eb-178a494e25fe");
        assertThat(outputMrcFile).containsOnlyOnce("s35473350-7c9b-48b0-86eb-178a494e25fe");

        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("s222222ea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s333333ea-1111-4d1d-957d-0abcdd0e9acd");

        assertThat(StringUtils.countMatches(outputMrcFile, "999")).isEqualTo(11);
      });
    }
  }

  @SneakyThrows
  @Test
  void exportAllHoldingsNonSuppressedNoErrorsCustomHoldingsProfileTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();

      createCustomHoldingsJobProfile();

      var exportAllRequest = new ExportAllRequest().idType(ExportAllRequest.IdTypeEnum.HOLDING)
          .jobProfileId(CUSTOM_HOLDINGS_JOB_PROFILE_ID);
      dataExportAllService.postDataExportAll(exportAllRequest);

      await().atMost(4, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        assertEquals(11, jobExecution.getJobExecution().getProgress().getTotal());

        var fileToExport = String.format("%s-%s.mrc", "holding-all", jobExecution.getJobExecution().getHrId());
        var s3path = getPathToStoredFiles(jobExecution.getId(), fileToExport);
        String outputMrcFile = Files.read(s3Client.read(s3path), Charset.defaultCharset());

        // Check FOLIO
        // Not suppressed:
        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i1089f777-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i1798f777-f243-4e4a-bf1c-9e1e62b3171d");

        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i0c45bb50-7c9b-48b0-86eb-178a494e25fe");
        assertThat(outputMrcFile).containsOnlyOnce("i1f45bb50-7c9b-48b0-86eb-178a494e25fe");

        // Check MARC
        // Not suppressed:
        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i55555577-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i66666677-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i91234977-f243-4e4a-bf1c-9e1e62b3171d");
        assertThat(outputMrcFile).containsOnlyOnce("i25462250-7c9b-48b0-86eb-178a494e25fe");
        assertThat(outputMrcFile).containsOnlyOnce("i35463350-7c9b-48b0-86eb-178a494e25fe");

        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i22222250-7c9b-48b0-86eb-178a494e25fe");
        assertThat(outputMrcFile).containsOnlyOnce("i33333350-7c9b-48b0-86eb-178a494e25fe");

        assertThat(StringUtils.countMatches(outputMrcFile, "999")).isEqualTo(11);

        removeCustomHoldingsJobProfile();
      });
    }
  }

  @SneakyThrows
  @Test
  void exportAllHoldingsNonDeletedNoErrorsTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();
      var exportAllRequest = new ExportAllRequest().idType(ExportAllRequest.IdTypeEnum.HOLDING)
          .jobProfileId(DEFAULT_HOLDINGS_JOB_PROFILE).suppressedFromDiscovery(true).deletedRecords(false);
      dataExportAllService.postDataExportAll(exportAllRequest);
      await().atMost(2, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        assertEquals(6, jobExecution.getJobExecution().getProgress().getTotal());

        var fileToExport = String.format("%s-%s.mrc", "holding-all", jobExecution.getJobExecution().getHrId());
        var s3path = getPathToStoredFiles(jobExecution.getId(), fileToExport);
        String outputMrcFile = Files.read(s3Client.read(s3path), Charset.defaultCharset());

        // Check FOLIO
        // Suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i1033bb50-7c9b-48b0-86eb-178a494e25fe");

        // Not suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i0c45bb50-7c9b-48b0-86eb-178a494e25fe");
        assertThat(outputMrcFile).containsOnlyOnce("i1f45bb50-7c9b-48b0-86eb-178a494e25fe");

        // Check MARC
        // Suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("s111111ea-1111-4d1d-957d-0abcdd0e9acd");

        // Not suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("s222222ea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s333333ea-1111-4d1d-957d-0abcdd0e9acd");

        assertThat(StringUtils.countMatches(outputMrcFile, "999")).isEqualTo(6);
      });
    }
  }

  @SneakyThrows
  @Test
  void exportAllHoldingsNonDeletedNoErrorsCustomHoldingsProfileTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();

      createCustomHoldingsJobProfile();

      var exportAllRequest = new ExportAllRequest().idType(ExportAllRequest.IdTypeEnum.HOLDING)
          .jobProfileId(CUSTOM_HOLDINGS_JOB_PROFILE_ID).suppressedFromDiscovery(true).deletedRecords(false);
      dataExportAllService.postDataExportAll(exportAllRequest);

      await().atMost(4, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        assertEquals(6, jobExecution.getJobExecution().getProgress().getTotal());

        var fileToExport = String.format("%s-%s.mrc", "holding-all", jobExecution.getJobExecution().getHrId());
        var s3path = getPathToStoredFiles(jobExecution.getId(), fileToExport);
        String outputMrcFile = Files.read(s3Client.read(s3path), Charset.defaultCharset());

        // Check FOLIO
        // Suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i1033bb50-7c9b-48b0-86eb-178a494e25fe");

        // Not suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i0c45bb50-7c9b-48b0-86eb-178a494e25fe");
        assertThat(outputMrcFile).containsOnlyOnce("i1f45bb50-7c9b-48b0-86eb-178a494e25fe");

        // Check MARC
        // Suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i11111150-7c9b-48b0-86eb-178a494e25fe");

        // Not suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i22222250-7c9b-48b0-86eb-178a494e25fe");
        assertThat(outputMrcFile).containsOnlyOnce("i33333350-7c9b-48b0-86eb-178a494e25fe");

        assertThat(StringUtils.countMatches(outputMrcFile, "999")).isEqualTo(6);

        removeCustomHoldingsJobProfile();
      });
    }
  }

  @SneakyThrows
  @Test
  void exportAllHoldingsNonSuppressedNonDeletedNoErrorsTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();
      var exportAllRequest = new ExportAllRequest().idType(ExportAllRequest.IdTypeEnum.HOLDING)
          .jobProfileId(DEFAULT_HOLDINGS_JOB_PROFILE).deletedRecords(false);
      dataExportAllService.postDataExportAll(exportAllRequest);

      await().atMost(4, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        assertEquals(4, jobExecution.getJobExecution().getProgress().getTotal());

        var fileToExport = String.format("%s-%s.mrc", "holding-all", jobExecution.getJobExecution().getHrId());
        var s3path = getPathToStoredFiles(jobExecution.getId(), fileToExport);
        String outputMrcFile = Files.read(s3Client.read(s3path), Charset.defaultCharset());

        // Check FOLIO
        // Not suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i0c45bb50-7c9b-48b0-86eb-178a494e25fe");
        assertThat(outputMrcFile).containsOnlyOnce("i1f45bb50-7c9b-48b0-86eb-178a494e25fe");

        // Check MARC
        // Not suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("s222222ea-1111-4d1d-957d-0abcdd0e9acd");
        assertThat(outputMrcFile).containsOnlyOnce("s333333ea-1111-4d1d-957d-0abcdd0e9acd");

        assertThat(StringUtils.countMatches(outputMrcFile, "999")).isEqualTo(4);
      });
    }
  }

  @SneakyThrows
  @Test
  void exportAllHoldingsNonSuppressedNonDeletedNoErrorsCustomHoldingsProfileTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();

      createCustomHoldingsJobProfile();

      var exportAllRequest = new ExportAllRequest().idType(ExportAllRequest.IdTypeEnum.HOLDING)
          .jobProfileId(CUSTOM_HOLDINGS_JOB_PROFILE_ID).deletedRecords(false);
      dataExportAllService.postDataExportAll(exportAllRequest);

      await().atMost(4, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        assertEquals(4, jobExecution.getJobExecution().getProgress().getTotal());

        var fileToExport = String.format("%s-%s.mrc", "holding-all", jobExecution.getJobExecution().getHrId());
        var s3path = getPathToStoredFiles(jobExecution.getId(), fileToExport);
        String outputMrcFile = Files.read(s3Client.read(s3path), Charset.defaultCharset());

        // Check FOLIO
        // Not suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i0c45bb50-7c9b-48b0-86eb-178a494e25fe");
        assertThat(outputMrcFile).containsOnlyOnce("i1f45bb50-7c9b-48b0-86eb-178a494e25fe");

        // Check MARC
        // Not suppressed:
        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("i22222250-7c9b-48b0-86eb-178a494e25fe");
        assertThat(outputMrcFile).containsOnlyOnce("i33333350-7c9b-48b0-86eb-178a494e25fe");

        assertThat(StringUtils.countMatches(outputMrcFile, "999")).isEqualTo(4);

        removeCustomHoldingsJobProfile();
      });
    }
  }

  // Ignore suppressed - see comments in https://folio-org.atlassian.net/browse/MDEXP-621
  @SneakyThrows
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void exportAllAuthorityNoErrorsIgnoreSuppressedTest(boolean suppressedFromDiscovery) {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();
      var exportAllRequest = new ExportAllRequest().idType(ExportAllRequest.IdTypeEnum.AUTHORITY)
        .jobProfileId(DEFAULT_AUTHORITY_JOB_PROFILE).suppressedFromDiscovery(suppressedFromDiscovery);
      dataExportAllService.postDataExportAll(exportAllRequest);
      await().atMost(4, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        assertEquals(4, jobExecution.getJobExecution().getProgress().getTotal());

        var fileToExport = String.format("%s-%s.mrc", "authority-all", jobExecution.getJobExecution().getHrId());
        var s3path = getPathToStoredFiles(jobExecution.getId(), fileToExport);
        String outputMrcFile = Files.read(s3Client.read(s3path), Charset.defaultCharset());

        // Deleted:
        assertThat(outputMrcFile).containsOnlyOnce("s28eed93e-f9e2-4cb2-a52b-e9155acfc119");
        assertThat(outputMrcFile).containsOnlyOnce("s34eed93e-f9e2-4cb2-a52b-e9155acfc119");
        assertThat(outputMrcFile).containsOnlyOnce("s45eed93e-f9e2-4cb2-a52b-e9155acfc119");

        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("s17eed93e-f9e2-4cb2-a52b-e9155acfc119");

        assertThat(StringUtils.countMatches(outputMrcFile, "999")).isEqualTo(4);
      });
    }
  }

  // Ignore suppressed - see comments in https://folio-org.atlassian.net/browse/MDEXP-621
  @SneakyThrows
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void exportAllAuthorityNonDeletedNoErrorsTest(boolean suppressedFromDiscovery) {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();
      var exportAllRequest = new ExportAllRequest().idType(ExportAllRequest.IdTypeEnum.AUTHORITY)
        .jobProfileId(DEFAULT_AUTHORITY_JOB_PROFILE).deletedRecords(false).suppressedFromDiscovery(suppressedFromDiscovery);
      dataExportAllService.postDataExportAll(exportAllRequest);

      await().atMost(4, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        assertEquals(1, jobExecution.getJobExecution().getProgress().getTotal());
        assertEquals(1, jobExecution.getJobExecution().getProgress().getTotal());

        var fileDefinition = fileDefinitionEntityRepository.getFileDefinitionByJobExecutionId(jobExecution.getId().toString()).get(0).getFileDefinition();
        var expectedFileName = "authority-all.csv";
        assertEquals(expectedFileName, fileDefinition.getFileName());

        var fileToExport = String.format("%s-%s.mrc", "authority-all", jobExecution.getJobExecution().getHrId());
        var s3path = getPathToStoredFiles(jobExecution.getId(), fileToExport);
        String outputMrcFile = Files.read(s3Client.read(s3path), Charset.defaultCharset());

        // Non-deleted:
        assertThat(outputMrcFile).containsOnlyOnce("s17eed93e-f9e2-4cb2-a52b-e9155acfc119");
        assertThat(StringUtils.countMatches(outputMrcFile, "999")).isEqualTo(1);
      });
    }
  }

  private void createCustomInstanceJobProfile() {
    var customInstanceMappingProfile = new MappingProfile().id(CUSTOM_INSTANCE_MAPPING_PROFILE_ID).name("Custom Instance Mapping Profile")
        ._default(false).recordTypes(List.of(RecordTypes.INSTANCE));
    mappingProfileEntityRepository.save(new MappingProfileEntity().withMappingProfile(customInstanceMappingProfile)
        .withId(customInstanceMappingProfile.getId()).withName(customInstanceMappingProfile.getName()));
    var customInstanceJobProfile = new JobProfile().id(CUSTOM_INSTANCE_JOB_PROFILE_ID).name("Custom Instance Job Profile")
        ._default(false).mappingProfileId(CUSTOM_INSTANCE_MAPPING_PROFILE_ID);
    jobProfileEntityRepository.save(new JobProfileEntity().withJobProfile(customInstanceJobProfile)
        .withId(customInstanceJobProfile.getId()).withName(customInstanceJobProfile.getName())
        .withMappingProfileId(CUSTOM_INSTANCE_MAPPING_PROFILE_ID));
  }

  private void createCustomHoldingsJobProfile() {
    var customHoldingsMappingProfile = new MappingProfile().id(CUSTOM_HOLDINGS_MAPPING_PROFILE_ID).name("Custom Holdings Mapping Profile")
        ._default(false).recordTypes(List.of(org.folio.dataexp.domain.dto.RecordTypes.HOLDINGS));
    mappingProfileEntityRepository.save(new MappingProfileEntity().withMappingProfile(customHoldingsMappingProfile)
        .withId(customHoldingsMappingProfile.getId()).withName(customHoldingsMappingProfile.getName()));
    var customHoldingsJobProfile = new JobProfile().id(CUSTOM_HOLDINGS_JOB_PROFILE_ID).name("Custom Holdings Job Profile")
        ._default(false).mappingProfileId(CUSTOM_HOLDINGS_MAPPING_PROFILE_ID);
    jobProfileEntityRepository.save(new JobProfileEntity().withJobProfile(customHoldingsJobProfile)
        .withId(customHoldingsJobProfile.getId()).withName(customHoldingsJobProfile.getName())
        .withMappingProfileId(CUSTOM_HOLDINGS_MAPPING_PROFILE_ID));
  }

  private void removeCustomInstanceJobProfile() {
    jobExecutionEntityCqlRepository.deleteAll();
    jobProfileEntityRepository.deleteById(CUSTOM_INSTANCE_JOB_PROFILE_ID);
    mappingProfileEntityRepository.deleteById(CUSTOM_INSTANCE_MAPPING_PROFILE_ID);
  }

  private void removeCustomHoldingsJobProfile() {
    jobExecutionEntityCqlRepository.deleteAll();
    jobProfileEntityRepository.deleteById(CUSTOM_HOLDINGS_JOB_PROFILE_ID);
    mappingProfileEntityRepository.deleteById(CUSTOM_HOLDINGS_MAPPING_PROFILE_ID);
  }
}
