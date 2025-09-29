package org.folio.dataexp.service;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.exception.file.definition.UploadFileException;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.service.file.upload.FilesUploadService;
import org.folio.dataexp.service.validators.FileDefinitionValidator;
import org.folio.spring.FolioExecutionContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Service for managing file definitions and file uploads.
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class FileDefinitionsService {
  private final FileDefinitionEntityRepository fileDefinitionEntityRepository;
  private final JobExecutionService jobExecutionService;
  private final FilesUploadService filesUploadService;
  private final FileDefinitionValidator fileDefinitionValidator;
  private final FolioExecutionContext folioExecutionContext;

  /**
   * Creates and saves a new file definition, initializing metadata and job execution.
   *
   * @param fileDefinition The file definition to save.
   * @return The saved FileDefinition.
   */
  public FileDefinition postFileDefinition(FileDefinition fileDefinition) {
    if (isEmpty(fileDefinition.getId())) {
      fileDefinition.setId(UUID.randomUUID());
    }
    log.info("Post file definition by id {}", fileDefinition.getId());
    fileDefinitionValidator.validate(fileDefinition);
    var jobExecution = jobExecutionService.save(
        new JobExecution().status(JobExecution.StatusEnum.NEW)
    );
    fileDefinition.setJobExecutionId(jobExecution.getId());
    fileDefinition.setStatus(FileDefinition.StatusEnum.NEW);
    var now = new Date();
    fileDefinition.setMetadata(
        new Metadata().createdDate(now).updatedDate(now)
    );
    var entity = FileDefinitionEntity.builder()
        .id(fileDefinition.getId())
        .creationDate(LocalDateTime.now())
        .createdBy(folioExecutionContext.getUserId().toString())
        .fileDefinition(fileDefinition)
        .build();
    var saved = fileDefinitionEntityRepository.save(entity);
    return saved.getFileDefinition();
  }

  /**
   * Retrieves a file definition by its ID.
   *
   * @param fileDefinitionId The UUID of the file definition.
   * @return The FileDefinition.
   */
  public FileDefinition getFileDefinitionById(UUID fileDefinitionId) {
    return fileDefinitionEntityRepository.getReferenceById(fileDefinitionId).getFileDefinition();
  }

  /**
   * Uploads a file for the given file definition.
   *
   * @param fileDefinitionId The UUID of the file definition.
   * @param resource The file resource to upload.
   * @return The updated FileDefinition.
   * @throws UploadFileException if upload fails.
   */
  public FileDefinition uploadFile(UUID fileDefinitionId, Resource resource) {
    try {
      return filesUploadService.uploadFile(fileDefinitionId, resource);
    } catch (Exception e) {
      log.error("Error uploading file for file definition id {} {}", fileDefinitionId, e);
      filesUploadService.errorUploading(fileDefinitionId);
      throw new UploadFileException(e.getMessage());
    }
  }
}
