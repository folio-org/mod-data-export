package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.exception.FileExtensionException;
import org.folio.dataexp.exception.FileSizeException;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class DataExportService {

  private static final String CSV_FORMAT_EXTENSION = "csv";
  private static final String CQL_FORMAT_EXTENSION = "cql";

  private static final int MAX_FILE_SIZE = 500_000;

  private final FileDefinitionEntityRepository fileDefinitionEntityRepository;
  private final JobExecutionEntityRepository jobExecutionEntityRepository;

  public FileDefinition postFileDefinition(FileDefinition fileDefinition) {
    if (Objects.nonNull(fileDefinition.getSize()) && fileDefinition.getSize() > MAX_FILE_SIZE) {
      var errorMessage = String.format("File size is too large: '%d'. Please use file with size less than %d.", fileDefinition.getSize(), MAX_FILE_SIZE);
      log.error(errorMessage);
      throw new FileSizeException(errorMessage);
    }
    if (Objects.isNull(fileDefinition.getSize())) {
      log.error("Size of uploading file is null.");
    }
    var jobExecution = new JobExecution();
    jobExecution.setId(UUID.randomUUID());
    jobExecutionEntityRepository.save(JobExecutionEntity.builder()
      .id(jobExecution.getId()).jobExecution(jobExecution).build());

    fileDefinition.setJobExecutionId(jobExecution.getId());
    fileDefinition.setStatus(FileDefinition.StatusEnum.NEW);
    if (isNotValidFileNameExtension(fileDefinition.getFileName())) {
      var errorMessage = String.format("Incorrect file extension of %s", fileDefinition.getFileName());
      log.error(errorMessage);
      throw new FileExtensionException(errorMessage);
    }
    replaceCQLExtensionToCSV(fileDefinition);
    var entity = FileDefinitionEntity.builder()
      .id(fileDefinition.getId()).fileDefinition(fileDefinition).build();
    var saved = fileDefinitionEntityRepository.save(entity);
    return saved.getFileDefinition();
  }

  private boolean isNotValidFileNameExtension(String fileName) {
    return !FilenameUtils.isExtension(fileName.toLowerCase(), CSV_FORMAT_EXTENSION) && !FilenameUtils.isExtension(fileName.toLowerCase(), CQL_FORMAT_EXTENSION);
  }

  private void replaceCQLExtensionToCSV(FileDefinition fileDefinition) {
    String fileName = fileDefinition.getFileName();
    if (FileDefinition.UploadFormatEnum.CQL == fileDefinition.getUploadFormat() && FilenameUtils.isExtension(fileName.toLowerCase(), CQL_FORMAT_EXTENSION)) {
      fileDefinition.setFileName(FilenameUtils.getBaseName(fileName) + "." + CSV_FORMAT_EXTENSION);
    }
  }
}
