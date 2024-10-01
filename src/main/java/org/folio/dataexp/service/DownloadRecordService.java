package org.folio.dataexp.service;


import static org.folio.dataexp.service.export.Constants.OUTPUT_BUFFER_SIZE;
import static org.folio.dataexp.util.S3FilePathUtils.RECORD_LOCATION_PATH;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.exception.export.DownloadRecordException;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.service.export.LocalStorageWriter;
import org.folio.dataexp.service.export.S3ExportsUploader;
import org.folio.dataexp.service.export.strategies.AuthorityExportStrategy;
import org.folio.dataexp.service.export.strategies.JsonToMarcConverter;
import org.folio.dataexp.util.S3FilePathUtils;
import org.folio.spring.FolioExecutionContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Log4j2
public class DownloadRecordService {

  private final AuthorityExportStrategy authorityExportStrategy;
  private final MappingProfileEntityRepository mappingProfileEntityRepository;
  private final JsonToMarcConverter jsonToMarcConverter;
  private final S3ExportsUploader s3Uploader;
  private final InputFileProcessor inputFileProcessor;
  protected final FolioExecutionContext folioExecutionContext;

  public ByteArrayResource processAuthorityDownload(final UUID authorityId, boolean isUtf, String formatPostfix) {
    var dirName = authorityId.toString() + formatPostfix;
    var marcFileContent = getContentIfFileExists(dirName);
    if (marcFileContent.isEmpty()) {
      marcFileContent = generateAuthorityFileContent(authorityId, isUtf);
      saveMarcFile(dirName, marcFileContent);
    }
    return new ByteArrayResource(marcFileContent.getBytes());
  }

  private String getContentIfFileExists(final String dirName) {
    try {
      return inputFileProcessor.readMarcFile(dirName);
    } catch (IOException e) {
      log.error("getContentIfFileExists:: Error reading record from the storage in: {}", dirName);
      throw new DownloadRecordException(e.getMessage());
    }
  }

  private String generateAuthorityFileContent(final UUID authorityId, boolean isUtf) {
    var marcAuthority = getMarcAuthority(authorityId);
    var mappingProfile = mappingProfileEntityRepository.getReferenceById(
      UUID.fromString("5d636597-a59d-4391-a270-4e79d5ba70e3")).getMappingProfile();
    try {
      return jsonToMarcConverter.convertJsonRecordToMarcRecord(marcAuthority.getContent(), List.of(),
        mappingProfile, isUtf);
    } catch (IOException e) {
      log.error("createAuthorityFileContent :: Error generating content for authority with ID: {}", authorityId);
      throw new DownloadRecordException(e.getMessage());
    }
  }

  private MarcRecordEntity getMarcAuthority(final UUID authorityId) {
    var marcAuthorities = authorityExportStrategy.getMarcAuthorities(Set.of(authorityId));
    if (marcAuthorities.isEmpty()) {
      log.error("getMarcAuthority:: Couldn't find authority in db for ID: {}", authorityId);
      throw new DownloadRecordException("Couldn't find authority in db for ID: %s".formatted(authorityId));
    }
    marcAuthorities = authorityExportStrategy.handleDuplicatedDeletedAndUseLastGeneration(marcAuthorities);
    return marcAuthorities.get(0);
  }

  private void saveMarcFile(final String dirName, final String marcFileContent) {
    var localStorageWriterPath = saveMarcFileToLocalStorage(dirName, marcFileContent);
    uploadMarcFile(dirName, localStorageWriterPath);
  }

  private String saveMarcFileToLocalStorage(final String dirName, final String marcFileContent) {
    var localFileLocation = RECORD_LOCATION_PATH.formatted(dirName, dirName) + ".mrc";
    var localStorageWriterPath = S3FilePathUtils.getLocalStorageWriterPath(
      authorityExportStrategy.getExportTmpStorage(), localFileLocation);
    createDirectoryForLocalStorage(dirName);
    var localStorageWriter = new LocalStorageWriter(localStorageWriterPath, OUTPUT_BUFFER_SIZE);
    localStorageWriter.write(marcFileContent);
    localStorageWriter.close();
    return localStorageWriterPath;
  }

  private void createDirectoryForLocalStorage(final String dirName) {
    try {
      Files.createDirectories(
        Path.of(S3FilePathUtils.getTempDirForRecordId(authorityExportStrategy.getExportTmpStorage(), dirName)));
    } catch (IOException e) {
      log.error("createDirectoryForLocalStorage:: Can not create temp directory for record {}", dirName);
      throw new DownloadRecordException(e.getMessage());
    }
  }

  private void uploadMarcFile(String dirName, String localStorageWriterPath) {
    var fileToUpload = new File(localStorageWriterPath);
    try {
      s3Uploader.uploadSingleRecordById(dirName, fileToUpload);
    } catch (IOException e) {
      log.error("uploadMarcFile:: Error while upload marc file {} to remote storage", localStorageWriterPath);
      throw new DownloadRecordException(e.getMessage());
    }
  }
}
