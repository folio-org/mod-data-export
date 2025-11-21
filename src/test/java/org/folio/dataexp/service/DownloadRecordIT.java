package org.folio.dataexp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.folio.dataexp.BaseDataExportInitializerIT;
import org.folio.dataexp.domain.dto.IdType;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import org.folio.dataexp.domain.dto.Metadata;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.UserInfo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import java.util.Collections;

class DownloadRecordIT extends BaseDataExportInitializerIT {

  @Autowired
  private FolioS3Client s3Client;

  @Autowired
  private DownloadRecordService downloadRecordService;

  @MockitoBean
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
    when(mappingProfileEntityRepository
        .getReferenceById(UUID.fromString("5d636597-a59d-4391-a270-4e79d5ba70e3")))
            .thenReturn(mappingProfileEntity);
    when(mappingProfileEntityRepository
        .getReferenceById(UUID.fromString("25d81cbe-9686-11ea-bb37-0242ac130002")))
            .thenReturn(mappingProfileEntity);
  }

  @ParameterizedTest
  @MethodSource("providedData")
  void whenInstanceIsMissing_downloadShouldFail(IdType idType, String recordId,
      boolean isUtf, String postfix, String fileContent) {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var uuid = UUID.fromString(MISSING_RECORD_ID);
      Exception exception = assertThrows(DownloadRecordException.class, () -> {
        downloadRecordService.processRecordDownload(uuid, isUtf, postfix, idType, false);
      });
      assertEquals("Couldn't find %s in db for ID: %s".formatted(idType.toString().toLowerCase(),
          MISSING_RECORD_ID), exception.getMessage());
    }
  }

  @ParameterizedTest
  @MethodSource("providedData")
  void whenMarcFileDoesntExist_generateFileAndSaveInS3(IdType idType, String recordId,
      boolean isUtf, String postfix, String fileContent) throws IOException {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var filePath = "mod-data-export/download/%s/%s.mrc".formatted(recordId + postfix,
          recordId + postfix);
      s3Client.remove(filePath);
      var expectedResult =
          new InputStreamResource((new ByteArrayInputStream(fileContent.getBytes())));

      var actualResult = downloadRecordService.processRecordDownload(UUID.fromString(recordId),
          isUtf, postfix, idType, false);

      assertTrue(compareInputStreams(expectedResult, actualResult));
      assertEquals(filePath, s3Client.list(filePath).getFirst());
    }
  }

  @ParameterizedTest
  @MethodSource("providedData")
  void whenMarcFileExists_retrieveItFromS3(IdType idType, String recordId, boolean isUtf,
      String postfix, String fileContent) throws IOException {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var filePath = "mod-data-export/download/%s/%s.mrc".formatted(recordId
          + postfix, recordId + postfix);
      s3Client.write(filePath, new ByteArrayInputStream(fileContent.getBytes()));
      var expectedResult =
          new InputStreamResource((new ByteArrayInputStream(fileContent.getBytes())));

      var actualResult = downloadRecordService.processRecordDownload(UUID.fromString(recordId),
          isUtf, postfix, idType, false);

      assertTrue(compareInputStreams(expectedResult, actualResult));
      assertEquals(filePath, s3Client.list(filePath).getFirst());
    }
  }

  @Test
  void suppress999ffFieldIfParameterIsTrue() throws IOException {
    final String fileContent =
            "00237cam a2200073 i 4500001001400000008004100014373002900055999007900084"
            + "\u001Ein00000001098\u001E210701t20222022nyua   c      001 0 eng d\u001E  \u001F"
            + "aπανεπιστήμιο\u001Eff\u001Fs17eed93e-f9e2-4cb2-a52b-e9155acfc119\u001Fi4a090b0f-"
            + "9da3-40f1-ab17-33d6a1e3abae\u001E\u001D";
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var filePath = "mod-data-export/download/%s/%s.mrc".formatted(AUTHORITY_ID
              + "-utf", AUTHORITY_ID + "-utf");
      s3Client.write(filePath, new ByteArrayInputStream(fileContent.getBytes()));

      var actualResult = downloadRecordService.processRecordDownload(UUID.fromString(AUTHORITY_ID),
              true, "-utf", IdType.AUTHORITY, true);
      assertFalse(IOUtils.toString(actualResult.getInputStream(), StandardCharsets.UTF_8)
              .contains("999"));
    }
  }

  @Test
  void doNotSuppress999ffFieldIfParameterIsFalse() throws IOException {
    final String fileContent =
            "00237cam a2200073 i 4500001001400000008004100014373002900055999007900084"
            + "\u001Ein00000001098\u001E210701t20222022nyua   c      001 0 eng d\u001E  \u001F"
            + "aπανεπιστήμιο\u001Eff\u001Fs17eed93e-f9e2-4cb2-a52b-e9155acfc119\u001Fi4a090b0f-"
            + "9da3-40f1-ab17-33d6a1e3abae\u001E\u001D";
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var filePath = "mod-data-export/download/%s/%s.mrc".formatted(AUTHORITY_ID
              + "-utf", AUTHORITY_ID + "-utf");
      s3Client.write(filePath, new ByteArrayInputStream(fileContent.getBytes()));

      var actualResult = downloadRecordService.processRecordDownload(UUID.fromString(AUTHORITY_ID),
              true, "-utf", IdType.AUTHORITY, false);
      assertTrue(IOUtils.toString(actualResult.getInputStream(), StandardCharsets.UTF_8)
              .contains("999"));
    }
  }

  @Test
  @SneakyThrows
  void whenMarcFileDoesntExist_suppress999ffInGeneratedMarc() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var actualResult = downloadRecordService.processRecordDownload(UUID.fromString(AUTHORITY_ID),
              true, "-utf", IdType.AUTHORITY, true);
      assertFalse(IOUtils.toString(actualResult.getInputStream(), StandardCharsets.UTF_8)
              .contains("999"));
    }
  }

  @Test
  @SneakyThrows
  void whenMarcFileDoesntExist_doNotSuppress999ffInGeneratedMarc() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var actualResult = downloadRecordService.processRecordDownload(UUID.fromString(AUTHORITY_ID),
              true, "-utf", IdType.AUTHORITY, false);
      assertTrue(IOUtils.toString(actualResult.getInputStream(), StandardCharsets.UTF_8)
              .contains("999"));
    }
  }

  // Java
  @Test
  void whenMarcFileExists_nonUtf_suppress999ff_removesField() throws IOException {
    final String fileContent =
            "00235cam  2200073 i 4500001001400000008004100014373002700055999007900082"
            + "\u001Ein00000001098\u001E210701t20222022nyua   c      001 0 eng d\u001E  \u001Fa"
            + "\u001B(Ssapfslvx\"jolr\u001B(B\u001B(B\u001Eff\u001Fs17eed93e-f9e2-4cb2-a52b-e91"
            + "55acfc119\u001Fi4a090b0f-9da3-40f1-ab17-33d6a1e3abae\u001E\u001D";
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var filePath = "mod-data-export/download/%s/%s.mrc"
              .formatted(AUTHORITY_ID + "-marc8", AUTHORITY_ID + "-marc8");
      s3Client.write(filePath, new ByteArrayInputStream(fileContent.getBytes()));

      var actualResult = downloadRecordService.processRecordDownload(
              UUID.fromString(AUTHORITY_ID),
              false, // isUtf = false
              "-marc8",
              IdType.AUTHORITY,
              true // suppress999ff = true
      );

      assertFalse(IOUtils.toString(actualResult.getInputStream(), StandardCharsets.UTF_8)
              .contains("999"));
    }
  }

    @Test
  void testFromMappingProfileShouldMapAllFieldsWhenInputIsFullyPopulated() {
    // TestMate-471da5621bb58be032d62ce77cfb2175
    // Given
    var mappingProfileId = UUID.fromString("f0f6d967-735c-4471-98a2-3e06a558d059");
    var createdDate = new Date(1697796000000L); // 2023-10-20T10:00:00Z
    var updatedDate = new Date(1697799600000L); // 2023-10-20T11:00:00Z
    var createdByUserId = "a1b2c3d4-e5f6-7890-1234-567890abcdef";
    var updatedByUserId = "fedcba09-8765-4321-fedc-ba0987654321";
    var firstName = "John";
    var lastName = "Doe";
    var userInfo = new UserInfo();
    userInfo.setFirstName(firstName);
    userInfo.setLastName(lastName);
    var metadata = new Metadata();
    metadata.setCreatedDate(createdDate);
    metadata.setUpdatedDate(updatedDate);
    metadata.setCreatedByUserId(createdByUserId);
    metadata.setUpdatedByUserId(updatedByUserId);
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(mappingProfileId);
    mappingProfile.setName("Test Profile");
    mappingProfile.setDescription("A test description");
    mappingProfile.setRecordTypes(List.of(RecordTypes.SRS, RecordTypes.INSTANCE));
    mappingProfile.setOutputFormat(MappingProfile.OutputFormatEnum.MARC);
    mappingProfile.setMetadata(metadata);
    mappingProfile.setUserInfo(userInfo);
    // When
    var actualEntity = MappingProfileEntity.fromMappingProfile(mappingProfile);
    // Then
    assertEquals(mappingProfileId, actualEntity.getId());
    assertEquals(mappingProfile, actualEntity.getMappingProfile());
    assertEquals("Test Profile", actualEntity.getName());
    assertEquals("A test description", actualEntity.getDescription());
    assertEquals(LocalDateTime.of(2023, 10, 20, 10, 0, 0), actualEntity.getCreationDate());
    assertEquals(LocalDateTime.of(2023, 10, 20, 11, 0, 0), actualEntity.getUpdatedDate());
    assertEquals(createdByUserId, actualEntity.getCreatedBy());
    assertEquals(updatedByUserId, actualEntity.getUpdatedByUserId());
    assertEquals(firstName, actualEntity.getUpdatedByFirstName());
    assertEquals(lastName, actualEntity.getUpdatedByLastName());
    assertEquals("SRS,INSTANCE", actualEntity.getRecordTypes());
    assertEquals("MARC", actualEntity.getFormat());
  }

    @Test
  void testFromMappingProfileShouldGenerateIdWhenIdIsNull() {
    // TestMate-f02f728b280616296f91947676c9f5af
    // Given
    var newMappingProfile = new MappingProfile();
    newMappingProfile.setMetadata(new Metadata());
    newMappingProfile.setUserInfo(new UserInfo());
    // When
    var createdEntity = MappingProfileEntity.fromMappingProfile(newMappingProfile);
    // Then
    assertNotNull(newMappingProfile.getId());
    assertNotNull(createdEntity.getId());
    assertEquals(newMappingProfile.getId(), createdEntity.getId());
    assertSame(newMappingProfile, createdEntity.getMappingProfile());
    assertNull(createdEntity.getCreationDate());
    assertNull(createdEntity.getCreatedBy());
    assertNull(createdEntity.getUpdatedByFirstName());
  }

    @Test
  void testFromMappingProfileShouldHandleNullMetadataAndUserInfo() {
    // TestMate-646764d322e9cbdb444faf0b10351203
    // Given
    var mappingProfile = new MappingProfile();
    mappingProfile.setName("Test Profile with Nulls");
    mappingProfile.setMetadata(null);
    mappingProfile.setUserInfo(null);
    // When
    var resultEntity = MappingProfileEntity.fromMappingProfile(mappingProfile);
    // Then
    assertNotNull(mappingProfile.getId());
    assertEquals(mappingProfile.getId(), resultEntity.getId());
    assertEquals("Test Profile with Nulls", resultEntity.getName());
    assertNull(resultEntity.getCreationDate());
    assertNull(resultEntity.getCreatedBy());
    assertNull(resultEntity.getUpdatedDate());
    assertNull(resultEntity.getUpdatedByUserId());
    assertNull(resultEntity.getUpdatedByFirstName());
    assertNull(resultEntity.getUpdatedByLastName());
  }

    @Test
  void testFromMappingProfileShouldHandlePartiallyNullMetadataAndUserInfo() {
    // TestMate-3e803a2e5095e5785825afae696be7c5
    // Given
    var mappingProfileId = UUID.fromString("1d8200b3-d25d-4a1c-95a2-933f114948a3");
    var createdDate = new Date(1697796000000L); // 2023-10-20T10:00:00Z
    var updatedByUserId = "fedcba09-8765-4321-fedc-ba0987654321";
    var firstName = "Jane";
    var metadata = new Metadata();
    metadata.setCreatedDate(createdDate);
    metadata.setUpdatedByUserId(updatedByUserId);
    metadata.setUpdatedDate(null);
    metadata.setCreatedByUserId(null);
    var userInfo = new UserInfo();
    userInfo.setFirstName(firstName);
    userInfo.setLastName(null);
    var mappingProfile = new MappingProfile();
    mappingProfile.setId(mappingProfileId);
    mappingProfile.setName("Partial Info Profile");
    mappingProfile.setMetadata(metadata);
    mappingProfile.setUserInfo(userInfo);
    // When
    var resultEntity = MappingProfileEntity.fromMappingProfile(mappingProfile);
    // Then
    assertEquals(mappingProfileId, resultEntity.getId());
    assertEquals(LocalDateTime.of(2023, 10, 20, 10, 0, 0), resultEntity.getCreationDate());
    assertEquals(updatedByUserId, resultEntity.getUpdatedByUserId());
    assertEquals(firstName, resultEntity.getUpdatedByFirstName());
    assertNull(resultEntity.getUpdatedDate());
    assertNull(resultEntity.getCreatedBy());
    assertNull(resultEntity.getUpdatedByLastName());
  }

    @ParameterizedTest
  @MethodSource("recordTypesScenarios")
  void testFromMappingProfileShouldCorrectlyMapRecordTypes(List<RecordTypes> inputRecordTypes, String expectedRecordTypesString) {
    // TestMate-1c436fb49d8962f383750084d1027f4f
    // Given
    var mappingProfile = new MappingProfile();
    mappingProfile.setRecordTypes(inputRecordTypes);
    mappingProfile.setMetadata(new Metadata());
    mappingProfile.setUserInfo(new UserInfo());
    // When
    var actualEntity = MappingProfileEntity.fromMappingProfile(mappingProfile);
    // Then
    assertEquals(expectedRecordTypesString, actualEntity.getRecordTypes());
  }

    @Test
  void testFromMappingProfileShouldHandleNullOutputFormat() {
    // TestMate-cd36d89d9860c06b0554f1d959524d0f
    // Given
    var mappingProfile = new MappingProfile();
    mappingProfile.setName("Profile with Null Output Format");
    mappingProfile.setOutputFormat(null);
    mappingProfile.setMetadata(new Metadata());
    mappingProfile.setUserInfo(new UserInfo());
    // When
    var resultEntity = MappingProfileEntity.fromMappingProfile(mappingProfile);
    // Then
    assertNull(resultEntity.getFormat());
    assertNotNull(resultEntity.getId());
  }

  private static Stream<Arguments> providedData() {
    return Stream.of(
      Arguments.of(IdType.AUTHORITY, AUTHORITY_ID, true,
          "-utf", "00237cam a2200073 i 4500001001400000008004100014373002900055999007900084"
          + "\u001Ein00000001098\u001E210701t20222022nyua   c      001 0 eng d\u001E  \u001F"
          + "aπανεπιστήμιο\u001Eff\u001Fs17eed93e-f9e2-4cb2-a52b-e9155acfc119\u001Fi4a090b0f-"
          + "9da3-40f1-ab17-33d6a1e3abae\u001E\u001D"),
      Arguments.of(IdType.AUTHORITY, AUTHORITY_ID, false,
          "-marc8", "00235cam  2200073 i 4500001001400000008004100014373002700055999007900082"
          + "\u001Ein00000001098\u001E210701t20222022nyua   c      001 0 eng d\u001E  \u001Fa"
          + "\u001B(Ssapfslvx\"jolr\u001B(B\u001B(B\u001Eff\u001Fs17eed93e-f9e2-4cb2-a52b-e91"
          + "55acfc119\u001Fi4a090b0f-9da3-40f1-ab17-33d6a1e3abae\u001E\u001D"),
      Arguments.of(IdType.INSTANCE, INSTANCE_ID, true, "-utf",
          "00221cam a2200061 i 4500001001400000008006600014999007900080\u001Ein00000001098"
          + "\u001E210701t20222022nyua   c      001 0 eng d πανεπιστήμιο\u001Eff\u001Fs717"
          + "1713e-f9e2-4cb2-a52b-e9155acfc119\u001Fi71717177-f243-4e4a-bf1c-9e1e62b3171d"
          + "\u001E\u001D"),
      Arguments.of(IdType.INSTANCE, INSTANCE_ID, false,
          "-marc8", "00219cam  2200061 i 4500001001400000008006400014999007900078\u001Ein00"
          + "000001098\u001E210701t20222022nyua   c      001 0 eng d \u001B(Ssapfslvx\"jolr"
          + "\u001B(B\u001B(B\u001Eff\u001Fs7171713e-f9e2-4cb2-a52b-e9155acfc119\u001Fi7171"
          + "7177-f243-4e4a-bf1c-9e1e62b3171d\u001E\u001D")
    );
  }

  public static boolean compareInputStreams(InputStreamResource isr1, InputStreamResource isr2)
      throws IOException {
    try (InputStream is1 = isr1.getInputStream(); InputStream is2 = isr2.getInputStream()) {
      return IOUtils.contentEquals(is1, is2);
    }
  }

    private static Stream<Arguments> recordTypesScenarios() {
    return Stream.of(
      Arguments.of(List.of(RecordTypes.SRS, RecordTypes.INSTANCE), "SRS,INSTANCE"),
      Arguments.of(List.of(RecordTypes.HOLDINGS), "HOLDINGS"),
      Arguments.of(Collections.emptyList(), ""),
      Arguments.of(null, null)
    );
  }
}
