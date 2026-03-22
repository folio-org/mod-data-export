package org.folio.dataexp.service.validators;

import static org.folio.dataexp.Constants.DEFAULT_DELETED_AUTHORITY_JOB_PROFILE;
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
import java.util.stream.Stream;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.domain.dto.ErrorLog;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.exception.export.DataExportRequestValidationException;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.util.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.folio.dataexp.util.ErrorCode.ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION;
import static org.folio.dataexp.util.ErrorCode.INVALID_UPLOADED_FILE_EXTENSION_FOR_HOLDING_ID_TYPE;

@ExtendWith(MockitoExtension.class)
class DataExportRequestValidatorTest {

  @Mock private ErrorLogService errorLogService;

  @InjectMocks private DataExportRequestValidator dataExportRequestValidator;

  private static Stream<Arguments> provideValidExportRequests() {
    var randomJobProfileId = UUID.fromString("00000000-0000-0000-0000-000000000000");
    var deletedJobProfileId = UUID.fromString("2c9be114-6d35-4408-adac-9ead35f51a27");
    var randomMappingProfileId = UUID.randomUUID().toString();
    return Stream.of(
        Arguments.of(
            ExportRequest.IdTypeEnum.INSTANCE,
            FileDefinition.UploadFormatEnum.CSV,
            randomMappingProfileId,
            randomJobProfileId),
        Arguments.of(
            ExportRequest.IdTypeEnum.HOLDING,
            FileDefinition.UploadFormatEnum.CSV,
            randomMappingProfileId,
            randomJobProfileId),
        Arguments.of(
            ExportRequest.IdTypeEnum.AUTHORITY,
            FileDefinition.UploadFormatEnum.CSV,
            DEFAULT_AUTTHORITY_PROFILE_ID,
            randomJobProfileId),
        Arguments.of(
            ExportRequest.IdTypeEnum.AUTHORITY,
            FileDefinition.UploadFormatEnum.CSV,
            DEFAULT_AUTTHORITY_PROFILE_ID,
            deletedJobProfileId));
  }

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
  void validateAuthorityDeletedProfileExportRequestTest() {
    when(errorLogService.saveGeneralErrorWithMessageValues(
            "error.onlyForSetToDeletion",
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
    assertThrows(
        DataExportRequestValidationException.class,
        () -> validator.validate(exportRequest, fileDefinition, "uuid"));

    exportRequest.setIdType(ExportRequest.IdTypeEnum.HOLDING);
    assertThrows(
        DataExportRequestValidationException.class,
        () -> validator.validate(exportRequest, fileDefinition, "uuid"));
  }

  @Test
  @TestMate(name = "TestMate-2fc8251d2068a6fd01c8f4df44bbb2ce")
  void validateAuthorityExportWithNonDefaultMappingProfileShouldThrowException() {
    // Given
    var jobExecutionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    var fileDefinition = new FileDefinition().jobExecutionId(jobExecutionId);
    var exportRequest =
        new ExportRequest()
            .idType(ExportRequest.IdTypeEnum.AUTHORITY)
            .jobProfileId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    var nonDefaultMappingProfileId = "non-default-mapping-profile-id";
    var errorCode = ErrorCode.ERROR_ONLY_DEFAULT_AUTHORITY_JOB_PROFILE_IS_SUPPORTED;
    when(errorLogService.saveGeneralErrorWithMessageValues(
            errorCode.getCode(),
            Collections.singletonList(errorCode.getDescription()),
            jobExecutionId))
        .thenReturn(new ErrorLog());
    // When
    var exception =
        assertThrows(
            DataExportRequestValidationException.class,
            () ->
                dataExportRequestValidator.validate(
                    exportRequest, fileDefinition, nonDefaultMappingProfileId));
    // Then
    assertEquals(
        "For exporting authority records only the default authority job profile is supported",
        exception.getMessage());
    verify(errorLogService)
        .saveGeneralErrorWithMessageValues(
            errorCode.getCode(),
            Collections.singletonList(errorCode.getDescription()),
            jobExecutionId);
  }

  @ParameterizedTest
  @TestMate(name = "TestMate-a8abdaa6265cad857cf8d0b07c4dab5d")
  @MethodSource("provideValidExportRequests")
  void validateShouldPassWhenRequestIsValid(
      ExportRequest.IdTypeEnum idType,
      FileDefinition.UploadFormatEnum uploadFormat,
      String mappingProfileId,
      UUID jobProfileId) {
    // Given
    var fileDefinition =
        new FileDefinition().jobExecutionId(UUID.randomUUID()).uploadFormat(uploadFormat);
    var exportRequest = new ExportRequest().idType(idType).jobProfileId(jobProfileId);
    // When & Then
    assertDoesNotThrow(
        () -> dataExportRequestValidator.validate(exportRequest, fileDefinition, mappingProfileId));
    verifyNoInteractions(errorLogService);
  }

    @Test
  void testValidateWhenMultipleErrorsOccurShouldConcatenateMessages() {
    // TestMate-655628fa2b5a3a918f36b325955ba2e9
    // Given
    var jobExecutionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    var deletedJobProfileId = UUID.fromString("2c9be114-6d35-4408-adac-9ead35f51a27");
    var mappingProfileId = "mapping-id";
    var fileDefinition = new FileDefinition()
        .jobExecutionId(jobExecutionId)
        .uploadFormat(FileDefinition.UploadFormatEnum.CQL);
    var exportRequest = new ExportRequest()
        .idType(ExportRequest.IdTypeEnum.HOLDING)
        .jobProfileId(deletedJobProfileId);
    when(errorLogService.saveGeneralErrorWithMessageValues(
        INVALID_UPLOADED_FILE_EXTENSION_FOR_HOLDING_ID_TYPE.getCode(),
        Collections.singletonList(INVALID_UPLOADED_FILE_EXTENSION_FOR_HOLDING_ID_TYPE.getDescription()),
        jobExecutionId))
        .thenReturn(new ErrorLog());
    when(errorLogService.saveGeneralErrorWithMessageValues(
        ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION.getCode(),
        List.of(ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION.getDescription()),
        jobExecutionId))
        .thenReturn(new ErrorLog());
    // When
    var exception = assertThrows(DataExportRequestValidationException.class,
        () -> dataExportRequestValidator.validate(exportRequest, fileDefinition, mappingProfileId));
    // Then
    var expectedMessage = "Only csv format is supported for holdings export; " + ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION.getDescription();
    assertEquals(expectedMessage, exception.getMessage());
    verify(errorLogService).saveGeneralErrorWithMessageValues(
        INVALID_UPLOADED_FILE_EXTENSION_FOR_HOLDING_ID_TYPE.getCode(),
        Collections.singletonList(INVALID_UPLOADED_FILE_EXTENSION_FOR_HOLDING_ID_TYPE.getDescription()),
        jobExecutionId);
    verify(errorLogService).saveGeneralErrorWithMessageValues(
        ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION.getCode(),
        List.of(ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION.getDescription()),
        jobExecutionId);
  }
}
