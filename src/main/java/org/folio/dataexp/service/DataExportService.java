package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.exception.export.DataExportException;
import org.folio.dataexp.exception.export.UploadFileException;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.repository.JobExecutionEntityRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.service.file.upload.FileUploadService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class DataExportService {

  private final FileDefinitionEntityRepository fileDefinitionEntityRepository;
  private final JobExecutionEntityRepository jobExecutionEntityRepository;
  private final JobProfileEntityRepository jobProfileEntityRepository;
  private final JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;
  private final FileUploadService fileUploadService;
  private final InputFileProcessor inputFileProcessor;
  private final SlicerProcessor slicerProcessor;
  private final SingleFileProcessor singleFileProcessor;
  private final FileDefinitionValidator fileDefinitionValidator;
  private final FolioExecutionContext folioExecutionContext;

  public FileDefinition postFileDefinition(FileDefinition fileDefinition) {
    log.info("Post file definition by id {}", fileDefinition.getId());
    fileDefinitionValidator.validate(fileDefinition);
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

  public void postDataExport(ExportRequest exportRequest) {
    log.info("Post data export for file definition {} and job profile {}", exportRequest.getFileDefinitionId(), exportRequest.getJobProfileId());
    var fileDefinition = fileDefinitionEntityRepository.
      getReferenceById(exportRequest.getFileDefinitionId()).getFileDefinition();
    var jobProfileEntity = jobProfileEntityRepository.getReferenceById(exportRequest.getJobProfileId());
    var jobExecutionEntity = jobExecutionEntityRepository.getReferenceById(fileDefinition.getJobExecutionId());
    var jobExecution = jobExecutionEntity.getJobExecution();
    jobExecution.setJobProfileId(jobProfileEntity.getJobProfile().getId());
    jobExecution.setJobProfileName(jobProfileEntity.getJobProfile().getName());
    jobExecutionEntityRepository.save(jobExecutionEntity);
    try {
      inputFileProcessor.readFile(fileDefinition);
      slicerProcessor.sliceInstancesIds(fileDefinition);
     } catch (Exception e) {
      throw new DataExportException(e.getMessage());
    }
    var jobExecutionExportFilesEntities = jobExecutionExportFilesEntityRepository.findByJobExecutionId(jobExecution.getId());
    singleFileProcessor.exportBySingleFile(jobExecutionExportFilesEntities, exportRequest.getRecordType());
  }
}
