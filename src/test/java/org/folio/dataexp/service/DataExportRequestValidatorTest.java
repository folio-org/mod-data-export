package org.folio.dataexp.service;

import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.exception.export.DataExportRequestValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.folio.dataexp.service.DataExportRequestValidator.DEFAULT_AUTHORITY_MAPPING_PROFILE_ID;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DataExportRequestValidatorTest {

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
    assertThrows(DataExportRequestValidationException.class, () -> validator.validate(exportRequest, fileDefinition, DEFAULT_AUTHORITY_MAPPING_PROFILE_ID));
  }
}
