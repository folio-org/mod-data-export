package org.folio.dataexp.service;

import com.github.jknack.handlebars.internal.Files;
import lombok.SneakyThrows;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.QuickExportRequest;
import org.folio.dataexp.exception.export.DataExportRequestValidationException;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class QuickExportServiceTest extends DataExportAllServiceTest {

  @Autowired
  private QuickExportService quickExportService;

  @MockBean
  private ConsortiaService consortiaService;

  @SneakyThrows
  @ParameterizedTest
  @CsvSource({"INSTANCE,011e1aea-222d-4d1d-957d-0abcdd0e9acd", "AUTHORITY,4a090b0f-9da3-40f1-ab17-33d6a1e3abae"})
  void quickExportNoErrorsTest(String recordType, String expectedId) {
    when(consortiaService.getCentralTenantId()).thenReturn("");
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      dataExportTenantService.loadReferenceData();
      handleReferenceData();
      var quickExportRequest = new QuickExportRequest()
        .uuids(List.of(UUID.fromString(expectedId)))
        .recordType(QuickExportRequest.RecordTypeEnum.fromValue(recordType)).type(QuickExportRequest.TypeEnum.UUID);
      var response = quickExportService.postQuickExport(quickExportRequest);

      assertNotNull(response);
      assertThat(response.getJobExecutionHrId()).isPositive();
      assertThat(response.getJobExecutionId()).isInstanceOf(UUID.class);

      var errors = errorLogEntityCqlRepository.findAll();
      assertThat(errors).isEmpty();
      var jobExecutions = jobExecutionEntityCqlRepository.findAll();
      assertThat(jobExecutions).hasSize(1);
      var jobExecution = jobExecutions.get(0);
      assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());

      var exportFiles = jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecution.getId());
      var fileLocation = exportFiles.get(0).getFileLocation();
      String outputMrcFile = Files.read(s3Client.read(fileLocation), Charset.defaultCharset());

      assertThat(outputMrcFile).containsOnlyOnce(expectedId);
    }
  }

  @SneakyThrows
  @Test
  void quickExport_shouldThrowErrorIfInvalidRecordTypeTest() {
    when(consortiaService.getCentralTenantId()).thenReturn("");
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      dataExportTenantService.loadReferenceData();
      handleReferenceData();
      var quickExportRequest = new QuickExportRequest()
        .uuids(List.of(UUID.fromString("4a090b0f-9da3-40f1-ab17-33d6a1e3abae")))
        .recordType(QuickExportRequest.RecordTypeEnum.ITEM).type(QuickExportRequest.TypeEnum.UUID);
      assertThrows(DataExportRequestValidationException.class, () -> quickExportService.postQuickExport(quickExportRequest));
    }
  }
}
