package org.folio.dataexp.service.validators;

import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.exception.export.DataExportRequestValidationException;
import org.springframework.stereotype.Component;

@Component
public class DataExportRequestValidator {

  public void validate(ExportRequest exportRequest, FileDefinition fileDefinition, String mappingProfileId) {
    if (exportRequest.getIdType() == ExportRequest.IdTypeEnum.HOLDING) {
      if (fileDefinition.getUploadFormat() == FileDefinition.UploadFormatEnum.CQL) {
        throw new DataExportRequestValidationException("Only csv format is supported for holdings export");
      }
    } else if (exportRequest.getIdType() == (ExportRequest.IdTypeEnum.AUTHORITY)) {
      if (!isDefaultAuthorityProfile(mappingProfileId)) {
        throw new DataExportRequestValidationException("For exporting authority records only the default authority job profile is supported");
      }
      if (fileDefinition.getUploadFormat() == FileDefinition.UploadFormatEnum.CQL) {
        throw new DataExportRequestValidationException("Only csv format is supported for authority export");
      }
    }
  }

  private boolean isDefaultAuthorityProfile(String mappingProfileId) {
    return StringUtils.equals(mappingProfileId, "5d636597-a59d-4391-a270-4e79d5ba70e3");
  }
}
