package org.folio.dataexp.service.file.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.QuickExportRequest;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.exception.UploadFileException;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.dataexp.service.export.storage.FolioS3ClientFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.codepipeline.model.JobData;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class FileUploadServiceImpl implements FileUploadService{

  private static final String PATTERN_TO_SAVE_FILE = "mod-data-export/%s/%s";
  private static final String ERROR_MESSAGE = "File already uploaded for file definition with id : ";

  private final FileDefinitionEntityRepository fileDefinitionEntityRepository;
  private final FolioS3ClientFactory folioS3ClientFactory;

  @Override
  public FileDefinition uploadFile(UUID fileDefinitionId, Resource resource) throws IOException {
    log.info("Upload file for file definition {}", fileDefinitionId);
    var fileDefinitionEntity = startUploading(fileDefinitionId);
    var s3Client = folioS3ClientFactory.getFolioS3Client();
    s3Client.write(String.format(PATTERN_TO_SAVE_FILE, fileDefinitionId.toString(), fileDefinitionEntity.getFileDefinition().getFileName()), resource.getInputStream());
    completeUploading(fileDefinitionEntity);
    log.info("Complete upload file for file definition {}", fileDefinitionId);
    return fileDefinitionEntity.getFileDefinition();
  }

  private FileDefinitionEntity startUploading(UUID fileDefinitionId) {
    var fileDefinitionEntity = fileDefinitionEntityRepository.getReferenceById(fileDefinitionId);
    var fileDefinition = fileDefinitionEntity.getFileDefinition();
    if (fileDefinition.getStatus() != FileDefinition.StatusEnum.NEW) {
      var errorMessage = ERROR_MESSAGE + fileDefinitionId;
      log.error(errorMessage);
      throw new UploadFileException(errorMessage);
    }
    fileDefinition.setStatus(FileDefinition.StatusEnum.IN_PROGRESS);
    fileDefinitionEntityRepository.save(fileDefinitionEntity);
    return fileDefinitionEntity;
  }

  private FileDefinition completeUploading(FileDefinitionEntity fileDefinitionEntity) {
    var fileDefinition = fileDefinitionEntity.getFileDefinition();
    fileDefinition.setStatus(FileDefinition.StatusEnum.COMPLETED);
    fileDefinitionEntityRepository.save(fileDefinitionEntity);
    return fileDefinition;
  }

  @Override
  public FileDefinition saveUUIDsByCQL(FileDefinition fileDefinition, String query) {
    return null;
  }

  @Override
  public FileDefinition errorUploading(UUID fileDefinitionId) {
    var fileDefinitionEntity = fileDefinitionEntityRepository.getReferenceById(fileDefinitionId);
    var fileDefinition = fileDefinitionEntity.getFileDefinition();
    fileDefinition.setStatus(FileDefinition.StatusEnum.ERROR);
    fileDefinitionEntityRepository.save(fileDefinitionEntity);
    return fileDefinition;
  }

  @Override
  public FileDefinition uploadFileDependsOnTypeForQuickExport(QuickExportRequest request, JobData jobData) {
    return null;
  }
}
