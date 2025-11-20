package org.folio.dataexp.service.validators;

import static org.folio.dataexp.BaseDataExportInitializerIT.DEFAULT_DELETED_AUTHORITY_JOB_PROFILE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.dataexp.domain.dto.ErrorLog;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.exception.export.DataExportRequestValidationException;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataExportRequestValidatorTest {

  @Mock
  private ErrorLogService errorLogService;

  @Test
  void validateHoldingExportRequestTest() {
    when(errorLogService.saveGeneralErrorWithMessageValues(
        "error.uploadedFile.invalidExtension",
        List.of("Only csv format is supported for holdings export"), null))
          .thenReturn(new ErrorLog());
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.fileName("upload.cql");
    fileDefinition.setSize(500_000);
    fileDefinition.setUploadFormat(FileDefinition.UploadFormatEnum.CQL);

    var exportRequest = new ExportRequest();
    exportRequest.setIdType(ExportRequest.IdTypeEnum.HOLDING);
    exportRequest.setJobProfileId(UUID.randomUUID());

    var validator = new DataExportRequestValidator(errorLogService);
    assertThrows(DataExportRequestValidationException.class, () ->
        validator.validate(exportRequest, fileDefinition, "uuid"));
  }

  @Test
  void validateAuthorityDeletedProfileExportRequestTest() {
    when(errorLogService.saveGeneralErrorWithMessageValues("error.onlyForSetToDeletion",
        List.of("This profile can only be used to export authority records set for deletion"),
        null))
            .thenReturn(new ErrorLog());
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());

    fileDefinition.setSize(500_000);

    var exportRequest = new ExportRequest();
    exportRequest.setIdType(ExportRequest.IdTypeEnum.INSTANCE);
    exportRequest.setJobProfileId(DEFAULT_DELETED_AUTHORITY_JOB_PROFILE);

    var validator = new DataExportRequestValidator(errorLogService);
    assertThrows(DataExportRequestValidationException.class, () ->
        validator.validate(exportRequest, fileDefinition, "uuid"));

    exportRequest.setIdType(ExportRequest.IdTypeEnum.HOLDING);
    assertThrows(DataExportRequestValidationException.class, () ->
        validator.validate(exportRequest, fileDefinition, "uuid"));
  }
}
