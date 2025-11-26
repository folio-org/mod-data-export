package org.folio.dataexp.service.validators;

import static org.folio.dataexp.service.export.strategies.AuthorityExportStrategy.DEFAULT_AUTTHORITY_PROFILE_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.folio.dataexp.domain.dto.ErrorLog;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.exception.export.DataExportRequestValidationException;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.util.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataExportRequestValidatorTest {

  private static final UUID DELETED_RECORDS_JOB_PROFILE_ID =
      UUID.fromString("2c9be114-6d35-4408-adac-9ead35f51a27");

  @Mock private ErrorLogService errorLogService;

  @Test
  void validateHoldingExportRequestTest() {
    when(errorLogService.saveGeneralErrorWithMessageValues(
            "error.uploadedFile.invalidExtension",
            List.of("Only csv format is supported for holdings export"),
            null))
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
    assertThrows(
        DataExportRequestValidationException.class,
        () -> validator.validate(exportRequest, fileDefinition, "uuid"));
  }

  @Test
  void validateShouldPassWhenIdTypeIsHoldingAndUploadFormatIsCsv() {
    // TestMate-b7c8b332968fe6b27727b87bc90062e5
    // Given
    var fileDefinition = new FileDefinition();
    fileDefinition.setUploadFormat(FileDefinition.UploadFormatEnum.CSV);
    var exportRequest = new ExportRequest();
    exportRequest.setIdType(ExportRequest.IdTypeEnum.HOLDING);
    exportRequest.setJobProfileId(UUID.randomUUID());
    var validator = new DataExportRequestValidator(errorLogService);
    // When & Then
    assertDoesNotThrow(
        () -> validator.validate(exportRequest, fileDefinition, UUID.randomUUID().toString()));
    verifyNoInteractions(errorLogService);
  }

  @Test
  void validateShouldThrowExceptionWhenIdTypeIsAuthorityAndMappingProfileIsNotDefault() {
    // TestMate-84a93746d155e1cb63a4b85954c352f9
    // Given
    var jobExecutionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    var fileDefinition = new FileDefinition();
    fileDefinition.setJobExecutionId(jobExecutionId);
    var exportRequest = new ExportRequest();
    exportRequest.setIdType(ExportRequest.IdTypeEnum.AUTHORITY);
    exportRequest.setJobProfileId(UUID.randomUUID());
    var nonDefaultMappingProfileId = "a1b2c3d4-e5f6-7890-1234-567890abcdef";
    var expectedErrorCode = ErrorCode.ERROR_ONLY_DEFAULT_AUTHORITY_JOB_PROFILE_IS_SUPPORTED;
    var expectedErrorMessage = expectedErrorCode.getDescription();
    when(errorLogService.saveGeneralErrorWithMessageValues(
            expectedErrorCode.getCode(),
            Collections.singletonList(expectedErrorMessage),
            jobExecutionId))
        .thenReturn(new ErrorLog());
    var validator = new DataExportRequestValidator(errorLogService);
    // When
    var exception =
        assertThrows(
            DataExportRequestValidationException.class,
            () -> validator.validate(exportRequest, fileDefinition, nonDefaultMappingProfileId));
    // Then
    assertEquals(expectedErrorMessage, exception.getMessage());
    verify(errorLogService)
        .saveGeneralErrorWithMessageValues(
            expectedErrorCode.getCode(),
            Collections.singletonList(expectedErrorMessage),
            jobExecutionId);
  }

  @Test
  void validateShouldPassWhenIdTypeIsAuthorityAndMappingProfileIsDefault() {
    // TestMate-19651f9ee2d481c1c9ac700d01bbaab4
    // Given
    var fileDefinition = new FileDefinition();
    var exportRequest = new ExportRequest();
    exportRequest.setIdType(ExportRequest.IdTypeEnum.AUTHORITY);
    exportRequest.setJobProfileId(UUID.randomUUID());
    var validator = new DataExportRequestValidator(errorLogService);
    // When & Then
    assertDoesNotThrow(
        () -> validator.validate(exportRequest, fileDefinition, DEFAULT_AUTTHORITY_PROFILE_ID));
    verifyNoInteractions(errorLogService);
  }

  @ParameterizedTest
  @EnumSource(
      value = ExportRequest.IdTypeEnum.class,
      names = {"AUTHORITY"},
      mode = EnumSource.Mode.EXCLUDE)
  void validateShouldThrowExceptionWhenJobProfileIsForDeletedRecordsAndIdTypeIsNotAuthority(
      ExportRequest.IdTypeEnum idType) {
    // TestMate-7cf7c679dd0c81eab0cf93d7378bae46
    // Given
    var jobExecutionId = UUID.randomUUID();
    var fileDefinition = new FileDefinition().jobExecutionId(jobExecutionId);
    var exportRequest =
        new ExportRequest().idType(idType).jobProfileId(DELETED_RECORDS_JOB_PROFILE_ID);
    var expectedErrorCode = ErrorCode.ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION;
    var expectedErrorMessage = expectedErrorCode.getDescription();
    when(errorLogService.saveGeneralErrorWithMessageValues(
            expectedErrorCode.getCode(), List.of(expectedErrorMessage), jobExecutionId))
        .thenReturn(new ErrorLog());
    var validator = new DataExportRequestValidator(errorLogService);
    // When
    var exception =
        assertThrows(
            DataExportRequestValidationException.class,
            () -> validator.validate(exportRequest, fileDefinition, UUID.randomUUID().toString()));
    // Then
    assertEquals(expectedErrorMessage, exception.getMessage());
    verify(errorLogService)
        .saveGeneralErrorWithMessageValues(
            expectedErrorCode.getCode(), List.of(expectedErrorMessage), jobExecutionId);
  }

  @Test
  void validateShouldPassWhenJobProfileIsForDeletedRecordsAndIdTypeIsAuthority() {
    // TestMate-f0859d65c539a47442d8946f1912dec8
    // Given
    var fileDefinition = new FileDefinition();
    var exportRequest =
        new ExportRequest()
            .idType(ExportRequest.IdTypeEnum.AUTHORITY)
            .jobProfileId(DELETED_RECORDS_JOB_PROFILE_ID);
    var validator = new DataExportRequestValidator(errorLogService);
    // When & Then
    assertDoesNotThrow(
        () -> validator.validate(exportRequest, fileDefinition, DEFAULT_AUTTHORITY_PROFILE_ID));
    verifyNoInteractions(errorLogService);
  }

  @Test
  void validateShouldPassForValidInstanceExportRequest() {
    // TestMate-32eec9c393213ba8c51b21e41fa1126e
    // Given
    var fileDefinition = new FileDefinition();
    fileDefinition.setUploadFormat(FileDefinition.UploadFormatEnum.CQL);
    var exportRequest = new ExportRequest();
    exportRequest.setIdType(ExportRequest.IdTypeEnum.INSTANCE);
    exportRequest.setJobProfileId(UUID.randomUUID());
    var validator = new DataExportRequestValidator(errorLogService);
    var mappingProfileId = UUID.randomUUID().toString();
    // When & Then
    assertDoesNotThrow(() -> validator.validate(exportRequest, fileDefinition, mappingProfileId));
    verifyNoInteractions(errorLogService);
  }
}
