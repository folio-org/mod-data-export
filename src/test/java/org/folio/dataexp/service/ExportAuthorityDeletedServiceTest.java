package org.folio.dataexp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.util.Constants.DEFAULT_AUTHORITY_DELETED_JOB_PROFILE_ID;
import static org.folio.dataexp.util.Constants.DELETED_AUTHORITIES_FILE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dataexp.TestMate;
import org.folio.dataexp.client.AuthorityClient;
import org.folio.dataexp.domain.dto.Authority;
import org.folio.dataexp.domain.dto.AuthorityCollection;
import org.folio.dataexp.domain.dto.ExportAuthorityDeletedRequest;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.exception.authority.AuthorityQueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

@ExtendWith(MockitoExtension.class)
class ExportAuthorityDeletedServiceTest {

  @Mock private DataExportService dataExportService;
  @Mock private AuthorityClient authorityClient;
  @Mock private FileDefinitionsService fileDefinitionsService;

  @InjectMocks private ExportAuthorityDeletedService exportAuthorityDeletedService;

  @Captor private ArgumentCaptor<ExportRequest> exportRequestArgumentCaptor;

  @Captor private ArgumentCaptor<Resource> resourceCaptor;

  @Captor private ArgumentCaptor<ExportRequest> exportRequestCaptor;

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
    when(fileDefinitionsService.postFileDefinition(any(FileDefinition.class)))
        .thenReturn(new FileDefinition().id(fileDefinition.getId()).jobExecutionId(jobExecutionId));
    when(fileDefinitionsService.uploadFile(any(UUID.class), any(Resource.class)))
        .thenReturn(
            new FileDefinition()
                .id(fileDefinition.getId())
                .jobExecutionId(jobExecutionId)
                .fileName(DELETED_AUTHORITIES_FILE_NAME));

    var response = exportAuthorityDeletedService.postExportDeletedAuthority(request);
    assertEquals(jobExecutionId, response.getJobExecutionId());
    verify(dataExportService).postDataExport(exportRequestArgumentCaptor.capture());
    var exportRequest = exportRequestArgumentCaptor.getValue();

    assertThat(exportRequest.getFileDefinitionId()).isInstanceOf(UUID.class);
    assertThat(exportRequest.getJobProfileId()).isInstanceOf(UUID.class);
  }

  @Test
  @TestMate(name = "TestMate-ce089722237a638589ba28036d2942f6")
  void testPostExportDeletedAuthorityWhenAuthoritiesFoundShouldCorrectlyFormatFileUploadContent() {
    // Given
    var authorityId1 = "authority-uuid-1";
    var authorityId2 = "authority-uuid-2";
    var authority1 = new Authority();
    authority1.setId(authorityId1);
    var authority2 = new Authority();
    authority2.setId(authorityId2);
    var authorityCollection = new AuthorityCollection();
    authorityCollection.setAuthorities(List.of(authority1, authority2));
    var request = new ExportAuthorityDeletedRequest();
    request.setQuery("id=all");
    request.setLimit(10);
    request.setOffset(0);
    var fileDefinitionId = UUID.fromString("00000000-1111-2222-3333-444444444444");
    var jobExecutionId = UUID.fromString("f0e9d8c7-b6a5-4321-fedc-ba9876543210");
    var fileDefinition =
        new FileDefinition()
            .id(fileDefinitionId)
            .jobExecutionId(jobExecutionId)
            .fileName(DELETED_AUTHORITIES_FILE_NAME);
    when(authorityClient.getAuthorities(true, true, "id=all", 10, 0))
        .thenReturn(authorityCollection);
    when(fileDefinitionsService.postFileDefinition(any(FileDefinition.class)))
        .thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(eq(fileDefinitionId), any(Resource.class)))
        .thenReturn(fileDefinition);
    // When
    var response = exportAuthorityDeletedService.postExportDeletedAuthority(request);
    // Then
    assertEquals(jobExecutionId, response.getJobExecutionId());
    verify(fileDefinitionsService).uploadFile(eq(fileDefinitionId), resourceCaptor.capture());
    var capturedResource = (ByteArrayResource) resourceCaptor.getValue();
    var expectedContent = authorityId1 + System.lineSeparator() + authorityId2;
    assertThat(new String(capturedResource.getByteArray())).isEqualTo(expectedContent);
    verify(dataExportService).postDataExport(exportRequestCaptor.capture());
    var capturedExportRequest = exportRequestCaptor.getValue();
    assertThat(capturedExportRequest.getFileDefinitionId()).isEqualTo(fileDefinitionId);
    assertThat(capturedExportRequest.getIdType()).isEqualTo(ExportRequest.IdTypeEnum.AUTHORITY);
  }

  @Test
  @TestMate(name = "TestMate-c5166343d16b1772953cfaeefaec8993")
  void testPostExportDeletedAuthorityWhenNoAuthoritiesFoundShouldCreateEmptyFileAndTriggerExport()
      throws IOException {
    // Given
    var request = new ExportAuthorityDeletedRequest();
    request.setQuery("id=none");
    request.setLimit(10);
    request.setOffset(0);
    var authorityCollection = new AuthorityCollection();
    authorityCollection.setAuthorities(Collections.emptyList());
    var fileDefinitionId = UUID.fromString("00000000-1111-2222-3333-444444444444");
    var jobExecutionId = UUID.fromString("f0e9d8c7-b6a5-4321-fedc-ba9876543210");
    var fileDefinition =
        new FileDefinition()
            .id(fileDefinitionId)
            .jobExecutionId(jobExecutionId)
            .fileName(DELETED_AUTHORITIES_FILE_NAME);
    when(authorityClient.getAuthorities(true, true, "id=none", 10, 0))
        .thenReturn(authorityCollection);
    when(fileDefinitionsService.postFileDefinition(any(FileDefinition.class)))
        .thenReturn(fileDefinition);
    when(fileDefinitionsService.uploadFile(eq(fileDefinitionId), any(Resource.class)))
        .thenReturn(fileDefinition);
    var fileDefCaptor = ArgumentCaptor.forClass(FileDefinition.class);
    // When
    var response = exportAuthorityDeletedService.postExportDeletedAuthority(request);
    // Then
    assertEquals(jobExecutionId, response.getJobExecutionId());
    verify(fileDefinitionsService).postFileDefinition(fileDefCaptor.capture());
    var capturedFileDef = fileDefCaptor.getValue();
    assertEquals(0, capturedFileDef.getSize());
    assertEquals(FileDefinition.UploadFormatEnum.CSV, capturedFileDef.getUploadFormat());
    assertEquals(DELETED_AUTHORITIES_FILE_NAME, capturedFileDef.getFileName());
    verify(fileDefinitionsService).uploadFile(eq(fileDefinitionId), resourceCaptor.capture());
    var capturedResource = (ByteArrayResource) resourceCaptor.getValue();
    assertThat(capturedResource.getByteArray()).isEmpty();
    verify(dataExportService).postDataExport(exportRequestCaptor.capture());
    var capturedExportRequest = exportRequestCaptor.getValue();
    assertThat(capturedExportRequest.getFileDefinitionId()).isEqualTo(fileDefinitionId);
    assertThat(capturedExportRequest.getIdType()).isEqualTo(ExportRequest.IdTypeEnum.AUTHORITY);
    assertThat(capturedExportRequest.getJobProfileId())
        .isEqualTo(UUID.fromString(DEFAULT_AUTHORITY_DELETED_JOB_PROFILE_ID));
  }

  @Test
  @TestMate(name = "TestMate-a1392a096ad9b7ae77f37e2c41899c67")
  void
      testPostExportDeletedAuthorityWhenDependencyThrowsExceptShouldThrowAuthorityQueryException() {
    // Given
    var request = new ExportAuthorityDeletedRequest();
    request.setQuery("id=error");
    request.setLimit(10);
    request.setOffset(0);
    var errorMessage = "Service Unavailable";
    when(authorityClient.getAuthorities(true, true, "id=error", 10, 0))
        .thenThrow(new RuntimeException(errorMessage));
    // When
    var exception =
        assertThrows(
            AuthorityQueryException.class,
            () -> exportAuthorityDeletedService.postExportDeletedAuthority(request));
    // Then
    assertEquals(errorMessage, exception.getMessage());
  }
}
