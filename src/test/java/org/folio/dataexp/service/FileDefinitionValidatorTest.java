package org.folio.dataexp.service;

import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.exception.export.FileExtensionException;
import org.folio.dataexp.exception.export.FileSizeException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class FileDefinitionValidatorTest {

  @Test
  void validateFileSizeTest() {
    var validator = new FileDefinitionValidator();
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.fileName("upload.csv");
    fileDefinition.setSize(500_001);

    assertThrows(FileSizeException.class, () -> validator.validate(fileDefinition));
  }

  @Test
  void validateFileExtensionTest() {
    var validator = new FileDefinitionValidator();
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.fileName("upload.txt");

    assertThrows(FileExtensionException.class, () -> validator.validate(fileDefinition));
  }
}
