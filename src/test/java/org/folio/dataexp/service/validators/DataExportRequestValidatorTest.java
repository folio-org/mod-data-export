package org.folio.dataexp.service.validators;

import org.folio.dataexp.domain.dto.ErrorLog;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.exception.export.DataExportRequestValidationException;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataExportRequestValidatorTest {

  @Mock
  private ErrorLogService errorLogService;

  @Test
  void validateHoldingExportRequestTest() {
    when(errorLogService.saveGeneralErrorWithMessageValues("error.uploadedFile.invalidExtension",
      List.of("Only csv format is supported for holdings export"), null))
      .thenReturn(new ErrorLog());
    var validator = new DataExportRequestValidator(errorLogService);
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.fileName("upload.cql");
    fileDefinition.setSize(500_000);
    fileDefinition.setUploadFormat(FileDefinition.UploadFormatEnum.CQL);

    var exportRequest = new ExportRequest();
    exportRequest.setIdType(ExportRequest.IdTypeEnum.HOLDING);

    assertThrows(DataExportRequestValidationException.class, () -> validator.validate(exportRequest, fileDefinition, "uuid"));
  }

  @Test
  void validateAuthorityExportRequestTest() {
    when(errorLogService.saveGeneralErrorWithMessageValues("error.messagePlaceholder",
      List.of("For exporting authority records only the default authority job profile is supported"), null))
      .thenReturn(new ErrorLog());
    var validator = new DataExportRequestValidator(errorLogService);
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());

    fileDefinition.setSize(500_000);

    var exportRequest = new ExportRequest();
    exportRequest.setIdType(ExportRequest.IdTypeEnum.AUTHORITY);

    assertThrows(DataExportRequestValidationException.class, () -> validator.validate(exportRequest, fileDefinition, "uuid"));

    fileDefinition.fileName("upload.cql");
    fileDefinition.setUploadFormat(FileDefinition.UploadFormatEnum.CQL);
    var defaultAuthorityMappingProfile = "5d636597-a59d-4391-a270-4e79d5ba70e3";
    assertThrows(DataExportRequestValidationException.class, () -> validator.validate(exportRequest, fileDefinition, defaultAuthorityMappingProfile));
  }
}
