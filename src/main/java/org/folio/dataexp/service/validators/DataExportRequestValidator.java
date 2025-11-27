package org.folio.dataexp.service.validators;

import static org.folio.dataexp.service.export.strategies.AuthorityExportStrategy.DEFAULT_AUTTHORITY_PROFILE_ID;
import static org.folio.dataexp.util.ErrorCode.ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.exception.export.DataExportRequestValidationException;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.util.ErrorCode;
import org.springframework.stereotype.Component;

/** Validator for export requests, including file definitions and mapping profiles. */
@Component
@RequiredArgsConstructor
public class DataExportRequestValidator {

  private final ErrorLogService errorLogService;

  /**
   * Validates the export request, file definition, and mapping profile. Throws {@link
   * DataExportRequestValidationException} if validation fails.
   *
   * @param exportRequest the export request
   * @param fileDefinition the file definition
   * @param mappingProfileId the mapping profile ID
   */
  public void validate(
      ExportRequest exportRequest, FileDefinition fileDefinition, String mappingProfileId) {
    String errorMsg = "";
    if (exportRequest.getIdType() == ExportRequest.IdTypeEnum.HOLDING) {
      if (fileDefinition.getUploadFormat() == FileDefinition.UploadFormatEnum.CQL) {
        errorLogService.saveGeneralErrorWithMessageValues(
            ErrorCode.INVALID_UPLOADED_FILE_EXTENSION_FOR_HOLDING_ID_TYPE.getCode(),
            Collections.singletonList(
                ErrorCode.INVALID_UPLOADED_FILE_EXTENSION_FOR_HOLDING_ID_TYPE.getDescription()),
            fileDefinition.getJobExecutionId());
        errorMsg = "Only csv format is supported for holdings export";
      }
    } else if (ExportRequest.IdTypeEnum.AUTHORITY.equals(exportRequest.getIdType())
        && !isDefaultAuthorityProfile(mappingProfileId)) {
      errorLogService.saveGeneralErrorWithMessageValues(
          ErrorCode.ERROR_ONLY_DEFAULT_AUTHORITY_JOB_PROFILE_IS_SUPPORTED.getCode(),
          Collections.singletonList(
              ErrorCode.ERROR_ONLY_DEFAULT_AUTHORITY_JOB_PROFILE_IS_SUPPORTED.getDescription()),
          fileDefinition.getJobExecutionId());
      errorMsg =
          "For exporting authority records only the default authority job profile is supported";
    }
    if (exportRequest.getIdType() != ExportRequest.IdTypeEnum.AUTHORITY
        && isDeletedJobProfile(exportRequest.getJobProfileId())) {
      var msg = ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION.getDescription();
      errorLogService.saveGeneralErrorWithMessageValues(
          ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION.getCode(),
          List.of(msg),
          fileDefinition.getJobExecutionId());
      if (!errorMsg.isEmpty()) {
        errorMsg += "; ";
      }
      errorMsg += msg;
    }
    if (!errorMsg.isEmpty()) {
      throw new DataExportRequestValidationException(errorMsg);
    }
  }

  /**
   * Checks if the mapping profile ID is the default authority profile.
   *
   * @param mappingProfileId the mapping profile ID
   * @return true if default, false otherwise
   */
  private boolean isDefaultAuthorityProfile(String mappingProfileId) {
    return StringUtils.equals(mappingProfileId, DEFAULT_AUTTHORITY_PROFILE_ID);
  }

  /**
   * Checks if the job profile ID is the deleted job profile.
   *
   * @param jobProfileId the job profile ID
   * @return true if deleted job profile, false otherwise
   */
  private boolean isDeletedJobProfile(UUID jobProfileId) {
    return StringUtils.equals(jobProfileId.toString(), "2c9be114-6d35-4408-adac-9ead35f51a27");
  }
}
