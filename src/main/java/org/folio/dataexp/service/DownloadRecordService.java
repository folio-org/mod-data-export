package org.folio.dataexp.service;


import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.entity.MarcRecordEntity;
import org.folio.dataexp.exception.export.DownloadRecordException;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.dataexp.service.export.S3ExportsUploader;
import org.folio.dataexp.service.export.strategies.AuthorityExportStrategy;
import org.folio.dataexp.service.export.strategies.JsonToMarcConverter;
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

  public ByteArrayResource processRecordDownload(final UUID recordId,
                                                 boolean isUtf,
                                                 final String formatPostfix,
                                                 final String idType) {
    if ("AUTHORITY".equals(idType)) {
      return processAuthorityDownload(recordId, isUtf, formatPostfix);
    }
    else {
      log.error("processRecordDownload:: unsupported record id type: {}", idType);
      throw new DownloadRecordException("Unsupported record id type: %s".formatted(idType));
    }
  }

  public ByteArrayResource processAuthorityDownload(final UUID authorityId, boolean isUtf, final String formatPostfix) {
    log.info("processAuthorityDownload:: start downloading authority with id: {}, isUtf: {}", authorityId, isUtf);
    var dirName = authorityId.toString() + formatPostfix;
    var marcFileContent = getContentIfFileExists(dirName);
    if (marcFileContent.isEmpty()) {
      marcFileContent = generateAuthorityFileContent(authorityId, isUtf);
      uploadMarcFile(dirName, marcFileContent);
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

  private void uploadMarcFile(final String dirName, final String marcFileContent) {
    try {
      s3Uploader.uploadSingleRecordById(dirName, marcFileContent);
    } catch (IOException e) {
      log.error("uploadMarcFile:: Error while upload marc file to remote storage {}", dirName);
      throw new DownloadRecordException(e.getMessage());
    }
  }
}
