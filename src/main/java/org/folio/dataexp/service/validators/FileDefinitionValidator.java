package org.folio.dataexp.service.validators;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.exception.file.definition.FileExtensionException;
import org.folio.dataexp.exception.file.definition.FileSizeException;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static org.folio.dataexp.util.ErrorCode.ERROR_FILE_BEING_UPLOADED_IS_TOO_LARGE;
import static org.folio.dataexp.util.ErrorCode.INVALID_UPLOADED_FILE_EXTENSION;

@Component
@Log4j2
@AllArgsConstructor
public class FileDefinitionValidator {

  private static final String CSV_FORMAT_EXTENSION = "csv";
  private static final String CQL_FORMAT_EXTENSION = "cql";

  private final ErrorLogService errorLogService;

  private static final int MAX_FILE_SIZE = 500_000;

  public void validate(FileDefinition fileDefinition) {
    if (Objects.nonNull(fileDefinition.getSize()) && fileDefinition.getSize() > MAX_FILE_SIZE) {
      var errorMessage = String.format("File size is too large: '%d'. Please use file with size less than %d.", fileDefinition.getSize(), MAX_FILE_SIZE);
      log.error(errorMessage);
      errorLogService.saveGeneralErrorWithMessageValues(ERROR_FILE_BEING_UPLOADED_IS_TOO_LARGE.getCode(), List.of(String.valueOf(fileDefinition.getSize())),
        fileDefinition.getJobExecutionId());
      throw new FileSizeException(errorMessage);
    }
    if (Objects.isNull(fileDefinition.getSize())) {
      log.error("Size of uploading file is null.");
    }
    if (isNotValidFileNameExtension(fileDefinition.getFileName())) {
      var errorMessage = String.format("Incorrect file extension of %s", fileDefinition.getFileName());
      log.error(errorMessage);
      errorLogService.saveGeneralErrorWithMessageValues(INVALID_UPLOADED_FILE_EXTENSION.getCode(), List.of(fileDefinition.getFileName()),
        fileDefinition.getJobExecutionId());
      throw new FileExtensionException(errorMessage);
    }
  }

  private boolean isNotValidFileNameExtension(String fileName) {
    return !FilenameUtils.isExtension(fileName.toLowerCase(), CSV_FORMAT_EXTENSION) && !FilenameUtils.isExtension(fileName.toLowerCase(), CQL_FORMAT_EXTENSION);
  }
}
