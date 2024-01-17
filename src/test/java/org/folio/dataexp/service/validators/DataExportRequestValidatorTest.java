package org.folio.dataexp.service.validators;

import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.exception.export.DataExportRequestValidationException;
import org.folio.dataexp.service.validators.DataExportRequestValidator;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DataExportRequestValidatorTest {

  @Test
  void validateHoldingExportRequestTest() {
    var validator = new DataExportRequestValidator();
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
    var validator = new DataExportRequestValidator();
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
