package org.folio.dataexp.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.IdType;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.exception.export.DownloadRecordException;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.s3.client.FolioS3Client;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;

class DownloadRecordTest extends BaseDataExportInitializer {

  @Autowired
  private FolioS3Client s3Client;

  @Autowired
  private DownloadRecordService downloadRecordService;

  @Autowired
  private InputFileProcessor inputFileProcessor;

  @MockBean
  private MappingProfileEntityRepository mappingProfileEntityRepository;

  private static final String AUTHORITY_ID = "4a090b0f-9da3-40f1-ab17-33d6a1e3abae";
  private static final String INSTANCE_ID = "71717177-f243-4e4a-bf1c-9e1e62b3171d";
  private static final String MISSING_RECORD_ID = "17eed93e-f9e2-4cb2-a52b-e9155acfc119";

  private final MappingProfileEntity mappingProfileEntity = new MappingProfileEntity();

  @BeforeEach
  void init() {
    var mappingProfile = new MappingProfile();
    mappingProfile.setDefault(true);
    mappingProfileEntity.setMappingProfile(mappingProfile);
    when(mappingProfileEntityRepository.getReferenceById(UUID.fromString("5d636597-a59d-4391-a270-4e79d5ba70e3")))
      .thenReturn(mappingProfileEntity);
    when(mappingProfileEntityRepository.getReferenceById(UUID.fromString("25d81cbe-9686-11ea-bb37-0242ac130002")))
      .thenReturn(mappingProfileEntity);
  }

  @ParameterizedTest
  @MethodSource("providedData")
  void whenInstanceIsMissing_downloadShouldFail(IdType idType, String recordId, boolean isUtf, String postfix,
    String fileContent) {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var uuid = UUID.fromString(MISSING_RECORD_ID);
      Exception exception = assertThrows(DownloadRecordException.class, () -> {
        downloadRecordService.processRecordDownload(uuid, isUtf, postfix, idType);
      });
      assertEquals("Couldn't find %s in db for ID: %s".formatted(idType.toString().toLowerCase(), MISSING_RECORD_ID),
        exception.getMessage());
    }
  }

  @ParameterizedTest
  @MethodSource("providedData")
  void whenMarcFileDoesntExist_generateFileAndSaveInS3(IdType idType, String recordId, boolean isUtf, String postfix,
    String fileContent) throws IOException {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var filePath = "mod-data-export/download/%s/%s.mrc".formatted(recordId + postfix, recordId + postfix);
      s3Client.remove(filePath);
      var expectedResult = new InputStreamResource((new ByteArrayInputStream(fileContent.getBytes())));

      var actualResult = downloadRecordService.processRecordDownload(UUID.fromString(recordId), isUtf, postfix, idType);

      assertTrue(compareInputStreams(expectedResult, actualResult));
      assertEquals(filePath, s3Client.list(filePath).get(0));
    }
  }

  @ParameterizedTest
  @MethodSource("providedData")
  void whenMarcFileExists_retrieveItFromS3(IdType idType, String recordId, boolean isUtf, String postfix,
    String fileContent) throws IOException {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var filePath = "mod-data-export/download/%s/%s.mrc".formatted(recordId + postfix, recordId + postfix);
      s3Client.write(filePath, new ByteArrayInputStream(fileContent.getBytes()));
      var expectedResult = new InputStreamResource((new ByteArrayInputStream(fileContent.getBytes())));

      var actualResult = downloadRecordService.processRecordDownload(UUID.fromString(recordId), isUtf, postfix, idType);

      assertTrue(compareInputStreams(expectedResult, actualResult));
      assertEquals(filePath, s3Client.list(filePath).get(0));
    }
  }

  private static Stream<Arguments> providedData() {
    return Stream.of(
      Arguments.of(IdType.AUTHORITY, AUTHORITY_ID, true, "-utf", "00237cam a2200073 i 4500001001400000008004100014373002900055999007900084\u001Ein00000001098\u001E210701t20222022nyua   c      001 0 eng d\u001E  \u001Faπανεπιστήμιο\u001Eff\u001Fs17eed93e-f9e2-4cb2-a52b-e9155acfc119\u001Fi4a090b0f-9da3-40f1-ab17-33d6a1e3abae\u001E\u001D"),
      Arguments.of(IdType.AUTHORITY, AUTHORITY_ID, false,"-marc8", "00235cam  2200073 i 4500001001400000008004100014373002700055999007900082\u001Ein00000001098\u001E210701t20222022nyua   c      001 0 eng d\u001E  \u001Fa\u001B(Ssapfslvx\"jolr\u001B(B\u001B(B\u001Eff\u001Fs17eed93e-f9e2-4cb2-a52b-e9155acfc119\u001Fi4a090b0f-9da3-40f1-ab17-33d6a1e3abae\u001E\u001D"),
      Arguments.of(IdType.INSTANCE, INSTANCE_ID, true, "-utf", "00221cam a2200061 i 4500001001400000008006600014999007900080\u001Ein00000001098\u001E210701t20222022nyua   c      001 0 eng d πανεπιστήμιο\u001Eff\u001Fs7171713e-f9e2-4cb2-a52b-e9155acfc119\u001Fi71717177-f243-4e4a-bf1c-9e1e62b3171d\u001E\u001D"),
      Arguments.of(IdType.INSTANCE, INSTANCE_ID, false,"-marc8", "00219cam  2200061 i 4500001001400000008006400014999007900078\u001Ein00000001098\u001E210701t20222022nyua   c      001 0 eng d \u001B(Ssapfslvx\"jolr\u001B(B\u001B(B\u001Eff\u001Fs7171713e-f9e2-4cb2-a52b-e9155acfc119\u001Fi71717177-f243-4e4a-bf1c-9e1e62b3171d\u001E\u001D")
    );
  }

  public static boolean compareInputStreams(InputStreamResource isr1, InputStreamResource isr2) throws IOException {
    try (InputStream is1 = isr1.getInputStream(); InputStream is2 = isr2.getInputStream()) {
      return IOUtils.contentEquals(is1, is2);
    }
  }
}
