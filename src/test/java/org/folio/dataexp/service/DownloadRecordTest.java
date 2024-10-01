package org.folio.dataexp.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.exception.export.DownloadRecordException;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.s3.client.FolioS3Client;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;

class DownloadRecordTest extends BaseDataExportInitializer {

  @Autowired
  private FolioS3Client s3Client;

  @Autowired
  private DownloadRecordService downloadRecordService;

  @Autowired
  private InputFileProcessor inputFileProcessor;

  @MockBean
  private MappingProfileEntityRepository mappingProfileEntityRepository;

  private final static UUID LOCAL_MARC_AUTHORITY_UUID = UUID.fromString("17eed93e-f9e2-4cb2-a52b-e9155acfc119");
  private final static UUID LOCAL_AUTHORITY_UUID = UUID.fromString("4a090b0f-9da3-40f1-ab17-33d6a1e3abae");

  private final MappingProfileEntity mappingProfileEntity = new MappingProfileEntity();

  @BeforeEach
  void init() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setDefault(true);
    mappingProfileEntity.setMappingProfile(mappingProfile);
    when(mappingProfileEntityRepository.getReferenceById(UUID.fromString("5d636597-a59d-4391-a270-4e79d5ba70e3")))
      .thenReturn(mappingProfileEntity);
  }

  @Test
  void whenAuthorityIsMissing_downloadShouldFail() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      Exception exception = assertThrows(DownloadRecordException.class, () -> {
        downloadRecordService.processAuthorityDownload(LOCAL_MARC_AUTHORITY_UUID, true, "-utf");
      });
      assertEquals("Couldn't find authority in db for ID: 17eed93e-f9e2-4cb2-a52b-e9155acfc119",
        exception.getMessage());
    }
  }

  @Test
  void whenUnknownRecordId_downloadShouldFail() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      Exception exception = assertThrows(DownloadRecordException.class, () -> {
        downloadRecordService.processRecordDownload(LOCAL_MARC_AUTHORITY_UUID, true, "-utf", "HOLDING");
      });
      assertEquals("Unsupported record id type: HOLDING",
        exception.getMessage());
    }
  }

  @ParameterizedTest
  @MethodSource("providedData")
  void whenMarcFileDoesntExist_generateFileAndSaveInS3(boolean isUtf, String postfix, String fileContent) {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var filePath = "mod-data-export/download/4a090b0f-9da3-40f1-ab17-33d6a1e3abae-%s/4a090b0f-9da3-40f1-ab17-33d6a1e3abae-%s.mrc".formatted(postfix, postfix);
      var expectedResult = new ByteArrayResource(fileContent.getBytes());

      var actualResult = downloadRecordService.processRecordDownload(LOCAL_AUTHORITY_UUID, isUtf, postfix, "AUTHORITY");

      assertEquals(expectedResult, actualResult);
      assertEquals(filePath, s3Client.list(filePath).get(0));
    }
  }

  @ParameterizedTest
  @MethodSource("providedData")
  void whenMarcFileExists_retrieveItFromS3(boolean isUtf, String postfix, String fileContent) {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var filePath = "mod-data-export/download/4a090b0f-9da3-40f1-ab17-33d6a1e3abae-%s/4a090b0f-9da3-40f1-ab17-33d6a1e3abae-%s.mrc".formatted(postfix, postfix);
      s3Client.write(filePath, new BufferedInputStream(new ByteArrayInputStream(fileContent.getBytes())));
      var expectedResult = new ByteArrayResource(fileContent.getBytes());

      var actualResult = downloadRecordService.processAuthorityDownload(LOCAL_AUTHORITY_UUID, isUtf, postfix);

      assertEquals(expectedResult, actualResult);
    }
  }

  private static Stream<Arguments> providedData() {
    return Stream.of(
      Arguments.of(true, "utf", "00237cam a2200073 i 4500001001400000008004100014373002900055999007900084\u001Ein00000001098\u001E210701t20222022nyua   c      001 0 eng d\u001E  \u001Faπανεπιστήμιο\u001Eff\u001Fs17eed93e-f9e2-4cb2-a52b-e9155acfc119\u001Fi4a090b0f-9da3-40f1-ab17-33d6a1e3abae\u001E\u001D"),
      Arguments.of(false,"marc8", "00235cam  2200073 i 4500001001400000008004100014373002700055999007900082\u001Ein00000001098\u001E210701t20222022nyua   c      001 0 eng d\u001E  \u001Fa\u001B(Ssapfslvx\"jolr\u001B(B\u001B(B\u001Eff\u001Fs17eed93e-f9e2-4cb2-a52b-e9155acfc119\u001Fi4a090b0f-9da3-40f1-ab17-33d6a1e3abae\u001E\u001D")
    );
  }
}
