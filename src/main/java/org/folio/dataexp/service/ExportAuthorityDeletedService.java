package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.client.AuthorityClient;
import org.folio.dataexp.domain.dto.Authority;
import org.folio.dataexp.domain.dto.ExportAuthorityDeletedRequest;
import org.folio.dataexp.domain.dto.ExportAuthorityDeletedResponse;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static org.folio.dataexp.util.Constants.DEFAULT_AUTHORITY_DELETED_JOB_PROFILE_ID;
import static org.folio.dataexp.util.Constants.DELETED_AUTHORITIES_FILE_NAME;

@Log4j2
@RequiredArgsConstructor
@Service
public class ExportAuthorityDeletedService {

  private final AuthorityClient authorityClient;

  private final FileDefinitionsService fileDefinitionsService;
  private final DataExportService dataExportService;

  public ExportAuthorityDeletedResponse postExportDeletedAuthority(ExportAuthorityDeletedRequest request) {
    log.info("POST export deleted authorities");
    var authorities = authorityClient.getAuthorities(true, true, request.getQuery(), request.getLimit(),
      request.getOffset());
    var fileDefinition = getFileDefinition(authorities.getAuthorities().stream().map(Authority::getId).toList());
    var exportRequest = ExportRequest.builder().fileDefinitionId(fileDefinition.getId()).jobProfileId(UUID.fromString(DEFAULT_AUTHORITY_DELETED_JOB_PROFILE_ID))
      .all(false).quick(false).idType(ExportRequest.IdTypeEnum.AUTHORITY).build();
    dataExportService.postDataExport(exportRequest);
    return ExportAuthorityDeletedResponse.builder().jobExecutionId(fileDefinition.getJobExecutionId()).build();
  }

  private FileDefinition getFileDefinition(List<String> authorityIds) {
    var fileContent = String.join(System.lineSeparator(), authorityIds);
    var fileDefinition = new FileDefinition();
    fileDefinition.setSize(authorityIds.size());
    fileDefinition.setUploadFormat(FileDefinition.UploadFormatEnum.CSV);
    fileDefinition.setFileName(DELETED_AUTHORITIES_FILE_NAME);
    fileDefinition = fileDefinitionsService.postFileDefinition(fileDefinition);
    fileDefinition = fileDefinitionsService.uploadFile(fileDefinition.getId(), new ByteArrayResource(fileContent.getBytes()));
    return fileDefinition;
  }
}
