package org.folio.dataexp.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.folio.dataexp.util.S3FilePathUtils.getPathToStoredFiles;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.github.jknack.handlebars.internal.Files;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.codehaus.plexus.util.StringUtils;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.QuickExportRequest;
import org.folio.dataexp.exception.export.DataExportRequestValidationException;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

class QuickExportServiceTest extends ServiceInitializer {

  @Autowired private QuickExportService quickExportService;

  private static final String FOLIO_INSTANCE_ID_NOT_DELETED_NOT_SUPPRESSED =
      "011e1aea-222d-4d1d-957d-0abcdd0e9acd";
  private static final String AUTHORITY_RECORD_EXTERNAL_ID_NOT_DELETED =
      "4a090b0f-9da3-40f1-ab17-33d6a1e3abae";

  @SneakyThrows
  @ParameterizedTest
  @CsvSource({
    "INSTANCE," + FOLIO_INSTANCE_ID_NOT_DELETED_NOT_SUPPRESSED,
    "AUTHORITY," + AUTHORITY_RECORD_EXTERNAL_ID_NOT_DELETED
  })
  void quickExportNoErrorsTest(String recordType, String expectedId) {
    when(consortiaService.getCentralTenantId(folioExecutionContext.getTenantId())).thenReturn("");
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();
      var quickExportRequest =
          new QuickExportRequest()
              .uuids(List.of(UUID.fromString(expectedId)))
              .recordType(QuickExportRequest.RecordTypeEnum.fromValue(recordType))
              .type(QuickExportRequest.TypeEnum.UUID);
      var response = quickExportService.postQuickExport(quickExportRequest);

      assertNotNull(response);
      assertThat(response.getJobExecutionHrId()).isPositive();
      assertThat(response.getJobExecutionId()).isInstanceOf(UUID.class);

      await()
          .atMost(4, SECONDS)
          .untilAsserted(
              () -> {
                var errors = errorLogEntityCqlRepository.findAll();
                assertThat(errors).isEmpty();
                var jobExecutions = jobExecutionEntityCqlRepository.findAll();
                assertThat(jobExecutions).hasSize(1);
                var jobExecution = jobExecutions.get(0);

                assertTrue(jobExecution.getJobExecution().getProgress().getTotal() > 0);
                assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());

                var fileToExport =
                    String.format("quick-export-%s.mrc", jobExecution.getJobExecution().getHrId());
                var s3path = getPathToStoredFiles(jobExecution.getId(), fileToExport);
                String outputMrcFile = Files.read(s3Client.read(s3path), Charset.defaultCharset());

                assertThat(outputMrcFile).containsOnlyOnce(expectedId);
                assertThat(StringUtils.countMatches(outputMrcFile, "999")).isEqualTo(1);
              });
    }
  }

  @SneakyThrows
  @Test
  void quickExport_shouldThrowErrorIfInvalidRecordTypeTest() {
    when(consortiaService.getCentralTenantId(folioExecutionContext.getTenantId())).thenReturn("");
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      dataExportTenantService.loadReferenceData();
      handleReferenceData();
      var quickExportRequest =
          new QuickExportRequest()
              .uuids(List.of(UUID.fromString(FOLIO_INSTANCE_ID_NOT_DELETED_NOT_SUPPRESSED)))
              .recordType(QuickExportRequest.RecordTypeEnum.ITEM)
              .type(QuickExportRequest.TypeEnum.UUID);
      assertThrows(
          DataExportRequestValidationException.class,
          () -> quickExportService.postQuickExport(quickExportRequest));
    }
  }
}
