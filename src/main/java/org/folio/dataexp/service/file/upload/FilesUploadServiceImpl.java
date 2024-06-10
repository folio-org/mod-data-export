package org.folio.dataexp.service.file.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.QuickExportRequest;
import org.folio.dataexp.domain.entity.FileDefinitionEntity;
import org.folio.dataexp.exception.file.definition.UploadFileException;
import org.folio.dataexp.repository.FileDefinitionEntityRepository;
import org.folio.s3.client.FolioS3Client;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

import static org.folio.dataexp.util.S3FilePathUtils.getPathToUploadedFiles;

@Service
@RequiredArgsConstructor
@Log4j2
public class FilesUploadServiceImpl implements FilesUploadService {


  private static final String ERROR_MESSAGE = "File already uploaded for file definition with id : ";

  private final FileDefinitionEntityRepository fileDefinitionEntityRepository;
  private final FolioS3Client s3Client;

  @Override
  public FileDefinition uploadFile(UUID fileDefinitionId, Resource resource) throws IOException {
    log.info("Upload file for file definition {}", fileDefinitionId);
    var fileDefinitionEntity = startUploading(fileDefinitionId);
    var fileName = fileDefinitionEntity.getFileDefinition().getFileName();
    var path = getPathToUploadedFiles(fileDefinitionId, fileName);
    try (var inputStream = resource != null ? resource.getInputStream() : InputStream.nullInputStream()) {
      s3Client.write(path, inputStream);
    }
    fileDefinitionEntity.setFileDefinition(fileDefinitionEntity.getFileDefinition().sourcePath(path));
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
    fileDefinition.setMetadata(fileDefinition.getMetadata().updatedDate(new Date()));
    fileDefinitionEntityRepository.save(fileDefinitionEntity);
    return fileDefinitionEntity;
  }

  private FileDefinition completeUploading(FileDefinitionEntity fileDefinitionEntity) {
    var fileDefinition = fileDefinitionEntity.getFileDefinition();
    fileDefinition.setStatus(FileDefinition.StatusEnum.COMPLETED);
    fileDefinition.setMetadata(fileDefinition.getMetadata().updatedDate(new Date()));
    fileDefinitionEntityRepository.save(fileDefinitionEntity);
    return fileDefinition;
  }

  @Override
  public FileDefinition errorUploading(UUID fileDefinitionId) {
    var fileDefinitionEntity = fileDefinitionEntityRepository.getReferenceById(fileDefinitionId);
    var fileDefinition = fileDefinitionEntity.getFileDefinition();
    fileDefinition.setStatus(FileDefinition.StatusEnum.ERROR);
    fileDefinition.setMetadata(fileDefinition.getMetadata().updatedDate(new Date()));
    fileDefinitionEntityRepository.save(fileDefinitionEntity);
    return fileDefinition;
  }
}
