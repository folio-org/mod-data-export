package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.exception.export.FileExtensionException;
import org.folio.dataexp.exception.export.FileSizeException;
import org.folio.dataexp.exception.export.UploadFileException;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.service.file.upload.FileUploadService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
  private final FileUploadService fileUploadService;
  private final FolioExecutionContext folioExecutionContext;

  public FileDefinition postFileDefinition(FileDefinition fileDefinition) {
    log.info("Post file definition by id {}", fileDefinition.getId());
    if (Objects.nonNull(fileDefinition.getSize()) && fileDefinition.getSize() > MAX_FILE_SIZE) {
      var errorMessage = String.format("File size is too large: '%d'. Please use file with size less than %d.", fileDefinition.getSize(), MAX_FILE_SIZE);
      log.error(errorMessage);
      throw new FileSizeException(errorMessage);
    }
    if (Objects.isNull(fileDefinition.getSize())) {
      log.error("Size of uploading file is null.");
    }
    if (isNotValidFileNameExtension(fileDefinition.getFileName())) {
      var errorMessage = String.format("Incorrect file extension of %s", fileDefinition.getFileName());
      log.error(errorMessage);
      throw new FileExtensionException(errorMessage);
    }
    var jobExecution = new JobExecution();
    jobExecution.setId(UUID.randomUUID());
    jobExecutionEntityRepository.save(JobExecutionEntity.builder()
      .id(jobExecution.getId()).jobExecution(jobExecution).build());
    fileDefinition.setJobExecutionId(jobExecution.getId());
    fileDefinition.setStatus(FileDefinition.StatusEnum.NEW);
    var entity = FileDefinitionEntity.builder()
      .id(fileDefinition.getId())
      .creationDate(LocalDateTime.now())
      .createdBy(folioExecutionContext.getUserId().toString())
      .fileDefinition(fileDefinition).build();
    var saved = fileDefinitionEntityRepository.save(entity);
    return saved.getFileDefinition();
  }

  public FileDefinition getFileDefinitionById(UUID fileDefinitionId) {
    return fileDefinitionEntityRepository.getReferenceById(fileDefinitionId).getFileDefinition();
  }

  public FileDefinition uploadFile(UUID fileDefinitionId, Resource resource) {
    try {
      return fileUploadService.uploadFile(fileDefinitionId, resource);
    } catch (Exception e) {
      fileUploadService.errorUploading(fileDefinitionId);
      throw new UploadFileException(e.getMessage());
    }
  }

  private boolean isNotValidFileNameExtension(String fileName) {
    return !FilenameUtils.isExtension(fileName.toLowerCase(), CSV_FORMAT_EXTENSION) && !FilenameUtils.isExtension(fileName.toLowerCase(), CQL_FORMAT_EXTENSION);
  }
}
