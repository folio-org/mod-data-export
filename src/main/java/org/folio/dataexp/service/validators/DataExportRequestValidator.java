package org.folio.dataexp.service.validators;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.exception.export.DataExportRequestValidationException;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.dataexp.util.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class DataExportRequestValidator {

  private final ErrorLogService errorLogService;

  public void validate(ExportRequest exportRequest, FileDefinition fileDefinition, String mappingProfileId) {
    String errorMsg = "";
    if (exportRequest.getIdType() == ExportRequest.IdTypeEnum.HOLDING) {
      if (fileDefinition.getUploadFormat() == FileDefinition.UploadFormatEnum.CQL) {
        errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.INVALID_UPLOADED_FILE_EXTENSION_FOR_HOLDING_ID_TYPE.getCode(),
          Collections.singletonList(ErrorCode.INVALID_UPLOADED_FILE_EXTENSION_FOR_HOLDING_ID_TYPE.getDescription()), fileDefinition.getJobExecutionId());
        errorMsg = "Only csv format is supported for holdings export";
      }
    } else if (exportRequest.getIdType() == (ExportRequest.IdTypeEnum.AUTHORITY)) {
      if (!isDefaultAuthorityProfile(mappingProfileId)) {
        errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.ERROR_ONLY_DEFAULT_AUTHORITY_JOB_PROFILE_IS_SUPPORTED.getCode(),
          Collections.singletonList(ErrorCode.ERROR_ONLY_DEFAULT_AUTHORITY_JOB_PROFILE_IS_SUPPORTED.getDescription()), fileDefinition.getJobExecutionId());
        errorMsg = "For exporting authority records only the default authority job profile is supported";
      }
      if (fileDefinition.getUploadFormat() == FileDefinition.UploadFormatEnum.CQL) {
        errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.INVALID_UPLOADED_FILE_EXTENSION_FOR_AUTHORITY_ID_TYPE.getCode(),
          Collections.singletonList(ErrorCode.INVALID_UPLOADED_FILE_EXTENSION_FOR_AUTHORITY_ID_TYPE.getDescription()), fileDefinition.getJobExecutionId());
        errorMsg = "Only csv format is supported for authority export";
      }
    }
    if (!errorMsg.isEmpty()) {
      throw new DataExportRequestValidationException(errorMsg);
    }
  }

  private boolean isDefaultAuthorityProfile(String mappingProfileId) {
    return StringUtils.equals(mappingProfileId, "5d636597-a59d-4391-a270-4e79d5ba70e3");
  }
}
