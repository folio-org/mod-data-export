package org.folio.dataexp.service;

import static org.folio.dataexp.util.Constants.DEFAULT_AUTHORITY_DELETED_JOB_PROFILE_ID;
import static org.folio.dataexp.util.Constants.DELETED_AUTHORITIES_FILE_NAME;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.client.AuthorityClient;
import org.folio.dataexp.domain.dto.Authority;
import org.folio.dataexp.domain.dto.ExportAuthorityDeletedRequest;
import org.folio.dataexp.domain.dto.ExportAuthorityDeletedResponse;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.exception.authority.AuthorityQueryException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

/** Service for exporting deleted authority records. */
@Log4j2
@RequiredArgsConstructor
@Service
public class ExportAuthorityDeletedService {

  private final AuthorityClient authorityClient;

  private final FileDefinitionsService fileDefinitionsService;
  private final DataExportService dataExportService;

  /**
   * Initiates export of deleted authority records.
   *
   * @param request The export request.
   * @return Response containing job execution ID.
   */
  public ExportAuthorityDeletedResponse postExportDeletedAuthority(
      ExportAuthorityDeletedRequest request) {
    log.info("POST export deleted authorities");
    try {
      var authorities =
          authorityClient.getAuthorities(
              true, true, request.getQuery(), request.getLimit(), request.getOffset());
      var fileDefinition =
          getFileDefinition(authorities.getAuthorities().stream().map(Authority::getId).toList());
      var exportRequest =
          ExportRequest.builder()
              .fileDefinitionId(fileDefinition.getId())
              .jobProfileId(UUID.fromString(DEFAULT_AUTHORITY_DELETED_JOB_PROFILE_ID))
              .all(false)
              .quick(false)
              .idType(ExportRequest.IdTypeEnum.AUTHORITY)
              .build();
      dataExportService.postDataExport(exportRequest);
      return ExportAuthorityDeletedResponse.builder()
          .jobExecutionId(fileDefinition.getJobExecutionId())
          .build();
    } catch (Exception e) {
      log.error(e);
      throw new AuthorityQueryException(e.getMessage());
    }
  }

  /**
   * Creates a FileDefinition for the given authority IDs.
   *
   * @param authorityIds List of authority IDs.
   * @return The created FileDefinition.
   */
  private FileDefinition getFileDefinition(List<String> authorityIds) {
    var fileDefinition = new FileDefinition();
    fileDefinition.setSize(authorityIds.size());
    fileDefinition.setUploadFormat(FileDefinition.UploadFormatEnum.CSV);
    fileDefinition.setFileName(DELETED_AUTHORITIES_FILE_NAME);
    fileDefinition = fileDefinitionsService.postFileDefinition(fileDefinition);
    var fileContent = String.join(System.lineSeparator(), authorityIds);
    fileDefinition =
        fileDefinitionsService.uploadFile(
            fileDefinition.getId(), new ByteArrayResource(fileContent.getBytes()));
    return fileDefinition;
  }
}
