package org.folio.dataexp.service.validators;

import org.folio.dataexp.domain.dto.ErrorLog;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.exception.file.definition.FileExtensionException;
import org.folio.dataexp.exception.file.definition.FileSizeException;
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
class FileDefinitionValidatorTest {

  @Mock
  private ErrorLogService errorLogService;

  @Test
  void validateFileSizeTest() {
    when(errorLogService.saveGeneralErrorWithMessageValues("error.fileIsTooLarge", List.of("500001"), null))
      .thenReturn(new ErrorLog());
    var validator = new FileDefinitionValidator(errorLogService);
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.fileName("upload.csv");
    fileDefinition.setSize(500_001);

    assertThrows(FileSizeException.class, () -> validator.validate(fileDefinition));
  }

  @Test
  void validateFileExtensionTest() {
    when(errorLogService.saveGeneralErrorWithMessageValues("error.uploadedFile.invalidExtension",
      List.of("upload.txt"), null)).thenReturn(new ErrorLog());
    var validator = new FileDefinitionValidator(errorLogService);
    var fileDefinition = new FileDefinition();
    fileDefinition.setId(UUID.randomUUID());
    fileDefinition.fileName("upload.txt");

    assertThrows(FileExtensionException.class, () -> validator.validate(fileDefinition));
  }
}
