package org.folio.dataexp.service;

import static org.folio.dataexp.util.Constants.DEFAULT_AUTHORITY_JOB_PROFILE_ID;
import static org.folio.dataexp.util.Constants.DEFAULT_HOLDINGS_JOB_PROFILE_ID;
import static org.folio.dataexp.util.Constants.DEFAULT_INSTANCE_JOB_PROFILE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.folio.dataexp.domain.dto.ExportAllRequest;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultJobProfileIdTest {

  @Mock private FileDefinitionsService mockFileDefinitionService;
  @Mock private DataExportService mockDataExportService;
  @InjectMocks private DataExportAllService mockDataExportAllService;

  @ParameterizedTest
  @EnumSource(value = ExportAllRequest.IdTypeEnum.class)
  void getDefaultJobProfileIdTest(ExportAllRequest.IdTypeEnum idType) {
    var exportAllRequest = new ExportAllRequest().idType(idType);
    mockDataExportAllService.postDataExportAll(exportAllRequest);
    var exportRequestArgumentCaptor = ArgumentCaptor.forClass(ExportRequest.class);
    verify(mockDataExportService).postDataExport(exportRequestArgumentCaptor.capture());
    String expected = null;
    if (idType == ExportAllRequest.IdTypeEnum.HOLDING) {
      expected = DEFAULT_HOLDINGS_JOB_PROFILE_ID;
    } else if (idType == ExportAllRequest.IdTypeEnum.AUTHORITY) {
      expected = DEFAULT_AUTHORITY_JOB_PROFILE_ID;
    } else if (idType == ExportAllRequest.IdTypeEnum.INSTANCE) {
      expected = DEFAULT_INSTANCE_JOB_PROFILE_ID;
    }
    assertEquals(
        UUID.fromString(expected), exportRequestArgumentCaptor.getValue().getJobProfileId());
  }
}
