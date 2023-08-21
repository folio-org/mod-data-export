package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.entity.ExportIdEntity;
import org.folio.dataexp.repository.ExportIdEntityRepository;
import org.folio.dataexp.service.export.storage.FolioS3ClientFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

import static org.folio.dataexp.service.file.upload.FileUploadServiceImpl.PATTERN_TO_SAVE_FILE;

@Component
@RequiredArgsConstructor
@Log4j2
public class InputFileProcessor {

  private final ExportIdEntityRepository exportIdEntityRepository;
  private final FolioS3ClientFactory folioS3ClientFactory;

  public void readCsvFile(FileDefinition fileDefinition) throws IOException {
    var pathToRead = String.format(PATTERN_TO_SAVE_FILE, fileDefinition.getId(), fileDefinition.getFileName());
    var s3Client = folioS3ClientFactory.getFolioS3Client();
    try (InputStream is = s3Client.read(pathToRead); BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
        reader.lines().forEach(id -> {
        var instanceId = id.replaceAll("\"", StringUtils.EMPTY);
        var entity = ExportIdEntity.builder().jobExecutionId(fileDefinition
          .getJobExecutionId()).instanceId(UUID.fromString(instanceId)).build();
        exportIdEntityRepository.save(entity);
      });
    }
  }
}
