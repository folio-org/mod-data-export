package org.folio.dataexp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.util.Constants.DELETED_AUTHORITIES_FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;
import org.folio.dataexp.client.AuthorityClient;
import org.folio.dataexp.domain.dto.AuthorityCollection;
import org.folio.dataexp.domain.dto.ExportAuthorityDeletedRequest;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ExportAuthorityDeletedServiceTest {

  @Mock
  private DataExportService dataExportService;
  @Mock
  private AuthorityClient authorityClient;
  @Mock
  private FileDefinitionsService fileDefinitionsService;

  @InjectMocks
  private ExportAuthorityDeletedService exportAuthorityDeletedService;

  @Captor
  private ArgumentCaptor<ExportRequest> exportRequestArgumentCaptor;

  @Test
  @SneakyThrows
  void postExportDeletedAuthorityTest() {
    var fileDefinition = new FileDefinition().size(1).id(UUID.randomUUID());
    UUID jobExecutionId = UUID.randomUUID();
    fileDefinition.setJobExecutionId(jobExecutionId);
    var request = new ExportAuthorityDeletedRequest();
    request.setOffset(0);
    request.setLimit(2);

    when(authorityClient.getAuthorities(true, true, null, 2, 0))
      .thenReturn(new AuthorityCollection());
    when(fileDefinitionsService.postFileDefinition(any(FileDefinition.class))).thenReturn(
      new FileDefinition().id(fileDefinition.getId()).jobExecutionId(jobExecutionId));
    when(fileDefinitionsService.uploadFile(any(UUID.class), any(Resource.class))).thenReturn(
      new FileDefinition().id(fileDefinition.getId()).jobExecutionId(jobExecutionId).fileName(DELETED_AUTHORITIES_FILE_NAME));

    var response = exportAuthorityDeletedService.postExportDeletedAuthority(request);

    verify(dataExportService).postDataExport(exportRequestArgumentCaptor.capture());
    var exportRequest = exportRequestArgumentCaptor.getValue();

    assertThat(exportRequest.getFileDefinitionId()).isInstanceOf(UUID.class);
    assertThat(exportRequest.getJobProfileId()).isInstanceOf(UUID.class);
    assertEquals(jobExecutionId, response.getJobExecutionId());
  }
}
