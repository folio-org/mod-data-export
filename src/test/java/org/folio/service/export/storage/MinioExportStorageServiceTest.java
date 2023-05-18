package org.folio.service.export.storage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.folio.config.ApplicationConfig;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.ExportedFile;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.s3.client.FolioS3Client;
import org.folio.s3.exception.S3ClientException;
import org.folio.service.logs.ErrorLogService;
import org.folio.spring.SpringContextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import io.minio.errors.*;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class MinioExportStorageServiceTest {

  public static final String TMP_DIR = "files";
  public static final String TMP_FILE_1 = "file-1.mrc";
  public static final String TMP_FILE_2 = "file-2.mrc";

  @Mock
  private FolioS3ClientFactory folioS3ClientFactory;
  @Mock
  private ErrorLogService errorLogService;
  @Spy
  private Vertx vertx = Vertx.vertx();
  @InjectMocks
  private ExportStorageService exportStorageService = new MinioStorageServiceImpl();

  @Captor
  private ArgumentCaptor<ErrorLog> errorLogCaptor;

  private final static String TENANT_ID = "testTenant";
  private final static String BUCKET_NAME = "test-bucket";

  public MinioExportStorageServiceTest() {
    Context vertxContext = vertx.getOrCreateContext();
    SpringContextUtil.init(vertxContext.owner(), vertxContext, ApplicationConfig.class);
    SpringContextUtil.autowireDependencies(this, vertxContext);
  }

  @BeforeEach
  void setUp() throws IOException {
    cleanUpTmpFiles();
    setUpTmpFiles();
    setBucket(BUCKET_NAME);
  }

  @AfterEach
  void tearDown() throws IOException {
    cleanUpTmpFiles();
  }

  private void setUpTmpFiles() throws IOException {
    Files.createDirectory(Paths.get(TMP_DIR));
    Files.createFile(Paths.get(TMP_DIR + "/" + TMP_FILE_1));
    Files.createFile(Paths.get(TMP_DIR + "/" + TMP_FILE_2));
  }

  private void cleanUpTmpFiles() throws IOException {
    Files.deleteIfExists(Paths.get(TMP_DIR + "/" + TMP_FILE_1));
    Files.deleteIfExists(Paths.get(TMP_DIR + "/" + TMP_FILE_2));
    Files.deleteIfExists(Paths.get(TMP_DIR));
  }

  private void setBucket(String bucketName) {
    try {
      var field = exportStorageService.getClass().getDeclaredField("bucket");
      field.setAccessible(true);
      field.set(exportStorageService, bucketName);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void storeFile_shouldPass() throws ServerException, InsufficientDataException, ErrorResponseException, IOException,
    NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, NoSuchFieldException {
    // given
    String jobId = UUID.randomUUID()
      .toString();
    String parentFolder = TENANT_ID + "/" + jobId;

    FileDefinition exportFileDefinition = new FileDefinition().withJobExecutionId(jobId)
      .withSourcePath(TMP_DIR + "/" + TMP_FILE_1);

    var client = Mockito.mock(FolioS3Client.class);
    when(folioS3ClientFactory.getFolioS3Client()).thenReturn(client);

    // when
    exportStorageService.storeFile(exportFileDefinition, TENANT_ID);

    // then
    Mockito.verify(client, Mockito.times(2))
      .write(any(), any());

  }

  @Test
  void storeFile_shouldFailIfBucketNameIsNotSet() {
    // given
    FileDefinition exportFileDefinition = new FileDefinition().withSourcePath(TMP_DIR + "/" + TMP_FILE_1);
    setBucket(null);

    // when
    Assertions.assertThrows(ServiceException.class, () -> {
      exportStorageService.storeFile(exportFileDefinition, TENANT_ID);
    });
    // then expect RuntimeException
  }

  @Test
  void storeFile_shouldFailOnUploadDirectory() throws ServerException, InsufficientDataException, ErrorResponseException,
    IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, NoSuchFieldException {
    // given
    FileDefinition exportFileDefinition = new FileDefinition().withSourcePath(TMP_DIR + "/" + TMP_FILE_1);
    var client = Mockito.mock(FolioS3Client.class);
    when(folioS3ClientFactory.getFolioS3Client()).thenReturn(client);
    Mockito.when(client.write(any(), any()))
      .thenThrow(new RuntimeException());

    // when
    Assertions.assertThrows(ServiceException.class, () -> {
      exportStorageService.storeFile(exportFileDefinition, TENANT_ID);
    });
    // then expect RuntimeException
  }

  @Test
  void testSuccessfulGenerateURL(VertxTestContext testContext) {
    // given
    String tenantId = "testAWS";
    String jobExecutionId = "67dfac11-1caf-4470-9ad1-d533f6360bdd";
    String fileId = "448ae575-daec-49c1-8041-d64c8ed8e5b1";

    var client = Mockito.mock(FolioS3Client.class);
    when(folioS3ClientFactory.getFolioS3Client()).thenReturn(client);

    var response = "https://test-aws-export-vk.s3.amazonaws.com";
    when(client.getPresignedUrl(any(String.class))).thenReturn(response);
    // when
    Future<String> linkFuture = exportStorageService.getFileDownloadLink(jobExecutionId, fileId, tenantId);
    // then
    linkFuture.onComplete(ar -> {
      testContext.verify(() -> {
        Assertions.assertTrue(ar.succeeded());
        Assertions.assertEquals(response, ar.result());
        testContext.completeNow();
      });

    });
  }

  @Test
  void testBucketNameNotFoundInS3(VertxTestContext testContext) {
    // given
    String jobExecutionId = "67dfac11-1caf-4470-9ad1-d533f6360bdd";
    String fileId = "448ae575-daec-49c1-8041-d64c8ed8e5b1";

    var client = Mockito.mock(FolioS3Client.class);
    when(folioS3ClientFactory.getFolioS3Client()).thenReturn(client);

    doThrow(new S3ClientException("Bucket Not Found")).when(client)
      .getPresignedUrl(any(String.class));

    // when
    Future<String> linkFuture = exportStorageService.getFileDownloadLink(jobExecutionId, fileId, TENANT_ID);
    // then
    linkFuture.onComplete(ar -> {
      testContext.verify(() -> {
        Assertions.assertTrue(ar.failed());
        Assertions.assertEquals("Bucket Not Found", ar.cause()
          .getMessage());
        testContext.completeNow();
      });
    });
  }

  @Test
  void testBucketNameNotProvidedInSystemProperty() {

    var client = Mockito.mock(FolioS3Client.class);
    when(folioS3ClientFactory.getFolioS3Client()).thenReturn(client);

    setBucket(null);

    Assertions.assertThrows(ServiceException.class, () -> {
      exportStorageService.getFileDownloadLink(null, null, null);
    });
  }

  @Test
  void testSuccessfullyRemoveFilesFromS3() throws NoSuchFieldException {
    // given
    ExportedFile exportedFile = new ExportedFile().withFileId(UUID.randomUUID()
      .toString())
      .withFileName("testFile-timestemp.mrc");
    JobExecution jobExecution = new JobExecution().withExportedFiles(Collections.singleton(exportedFile))
      .withId(UUID.randomUUID()
        .toString());
    var client = Mockito.mock(FolioS3Client.class);
    when(folioS3ClientFactory.getFolioS3Client()).thenReturn(client);
    when(client.list(isA(String.class))).thenReturn(List.of("path1", "path2"));
    // when
    exportStorageService.removeFilesRelatedToJobExecution(jobExecution, TENANT_ID);

    // then
    Mockito.verify(client, times(2)).remove(isA(String.class));
  }

  @Test
  void testBucketNameNotProvidedInSystemPropertyWhileRemovingFilesFromS3() {
    setBucket(null);

    Assertions.assertThrows(ServiceException.class, () -> {
      exportStorageService.removeFilesRelatedToJobExecution(null, null);
    });
  }

  @Test
  void testRemovingFilesFromS3ExportedFilesIsEmpty() throws NoSuchFieldException {
    // when
    exportStorageService.removeFilesRelatedToJobExecution(new JobExecution(), TENANT_ID);
    // then
    Mockito.verify(folioS3ClientFactory, never())
      .getFolioS3Client();
  }

}
