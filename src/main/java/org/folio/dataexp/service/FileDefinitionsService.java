package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.exception.file.definition.UploadFileException;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.service.file.upload.FileUploadService;
import org.folio.dataexp.service.validators.FileDefinitionValidator;
import org.folio.spring.FolioExecutionContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class FileDefinitionsService {
  private final FileDefinitionEntityRepository fileDefinitionEntityRepository;
  private final JobExecutionEntityRepository jobExecutionEntityRepository;
  private final FilesUploadService filesUploadService;
  private final FileDefinitionValidator fileDefinitionValidator;
  private final FolioExecutionContext folioExecutionContext;

  public FileDefinition postFileDefinition(FileDefinition fileDefinition) {
    log.info("Post file definition by id {}", fileDefinition.getId());
    fileDefinitionValidator.validate(fileDefinition);
    var jobExecution = new JobExecution();
    jobExecution.setId(UUID.randomUUID());
    jobExecution.setStatus(JobExecution.StatusEnum.NEW);
    jobExecutionEntityRepository.save(JobExecutionEntity.builder()
      .id(jobExecution.getId()).jobExecution(jobExecution).build());
    fileDefinition.setJobExecutionId(jobExecution.getId());
    fileDefinition.setStatus(FileDefinition.StatusEnum.NEW);
    var now = new Date();
    fileDefinition.setMetadata(new Metadata().createdDate(now).updatedDate(now));
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
      return filesUploadService.uploadFile(fileDefinitionId, resource);
    } catch (Exception e) {
      log.error("Error uploading file for file definition id {} {}", fileDefinitionId, e);
      filesUploadService.errorUploading(fileDefinitionId);
      throw new UploadFileException(e.getMessage());
    }
  }
}
