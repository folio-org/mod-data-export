package org.folio.service.export.storage;

import static org.folio.service.export.storage.MinioClientFactory.BUCKET_PROP_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.ExportedFile;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.rest.jaxrs.model.JobExecution;
import org.folio.service.logs.ErrorLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import io.minio.messages.Version;
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
  private MinioClientFactory minioClientFactory;
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

  @BeforeEach
  void setUp() throws IOException {
    System.setProperty(BUCKET_PROP_KEY, BUCKET_NAME);
    cleanUpTmpFiles();
    setUpTmpFiles();
  }

  @AfterEach
  void tearDown() throws IOException {
    System.clearProperty(BUCKET_PROP_KEY);
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

  @Test
  void storeFile_shouldPass() throws ServerException, InsufficientDataException, ErrorResponseException, IOException,
      NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
    // given
    String jobId = UUID.randomUUID()
      .toString();
    String parentFolder = TENANT_ID + "/" + jobId;

    FileDefinition exportFileDefinition = new FileDefinition().withJobExecutionId(jobId)
      .withSourcePath(TMP_DIR + "/" + TMP_FILE_1);

    var client = Mockito.mock(MinioClient.class);
    when(minioClientFactory.getClient()).thenReturn(client);

    // when
    exportStorageService.storeFile(exportFileDefinition, TENANT_ID);

    // then
    Mockito.verify(client, Mockito.times(2))
      .uploadObject(any(UploadObjectArgs.class));

  }

  @Test
  void storeFile_shouldFailIfBucketNameIsNotSet() {
    // given
    System.clearProperty(BUCKET_PROP_KEY);
    FileDefinition exportFileDefinition = new FileDefinition().withSourcePath(TMP_DIR + "/" + TMP_FILE_1);

    // when
    Assertions.assertThrows(ServiceException.class, () -> {
      exportStorageService.storeFile(exportFileDefinition, TENANT_ID);
    });
    // then expect RuntimeException
  }

  @Test
  void storeFile_shouldFailOnUploadDirectory() throws ServerException, InsufficientDataException, ErrorResponseException,
      IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
    // given
    FileDefinition exportFileDefinition = new FileDefinition().withSourcePath(TMP_DIR + "/" + TMP_FILE_1);
    var client = Mockito.mock(MinioClient.class);
    when(minioClientFactory.getClient()).thenReturn(client);
    Mockito.when(client.uploadObject(any(UploadObjectArgs.class)))
      .thenThrow(new RuntimeException());
    // when
    Assertions.assertThrows(ServiceException.class, () -> {
      exportStorageService.storeFile(exportFileDefinition, TENANT_ID);
    });
    // then expect RuntimeException
  }

  @Test
  void testSuccessfulGenerateURL(VertxTestContext testContext)
      throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException,
      InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
    // given
    String tenantId = "testAWS";
    String jobExecutionId = "67dfac11-1caf-4470-9ad1-d533f6360bdd";
    String fileId = "448ae575-daec-49c1-8041-d64c8ed8e5b1";

    var client = Mockito.mock(MinioClient.class);
    when(minioClientFactory.getClient()).thenReturn(client);

    var response = "https://test-aws-export-vk.s3.amazonaws.com";
    when(client.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn(response);
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
  void testBucketNameNotFoundInS3(VertxTestContext testContext)
      throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException,
      InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
    // given
    String jobExecutionId = "67dfac11-1caf-4470-9ad1-d533f6360bdd";
    String fileId = "448ae575-daec-49c1-8041-d64c8ed8e5b1";

    var client = Mockito.mock(MinioClient.class);
    when(minioClientFactory.getClient()).thenReturn(client);

    doThrow(new ServerException("Bucket Not Found", null)).when(client)
      .getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
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
    System.clearProperty(BUCKET_PROP_KEY);

    var client = Mockito.mock(MinioClient.class);
    when(minioClientFactory.getClient()).thenReturn(client);

    Assertions.assertThrows(ServiceException.class, () -> {
      exportStorageService.getFileDownloadLink(null, null, null);
    });
  }

  @Test
  void testSuccessfullyRemoveFilesFromS3() {
    // given
    ExportedFile exportedFile = new ExportedFile().withFileId(UUID.randomUUID()
      .toString())
      .withFileName("testFile-timestemp.mrc");
    JobExecution jobExecution = new JobExecution().withExportedFiles(Collections.singleton(exportedFile))
      .withId(UUID.randomUUID()
        .toString());
    var client = Mockito.mock(MinioClient.class);
    when(minioClientFactory.getClient()).thenReturn(client);

    when(client.listObjects(any(ListObjectsArgs.class))).thenReturn(() -> new Iterator<>() {

      private boolean hasNext = true;

      @Override
      public boolean hasNext() {
        try {
          return hasNext;
        } finally {
          hasNext = false;
        }
      }

      @Override
      public Result<Item> next() {
        return new Result<>(new Version());
      }
    });
    // when
    exportStorageService.removeFilesRelatedToJobExecution(jobExecution, TENANT_ID);

    // then
    Mockito.verify(client)
      .removeObjects(any(RemoveObjectsArgs.class));
  }

  @Test
  void testBucketNameNotProvidedInSystemPropertyWhileRemovingFilesFromS3() {
    System.clearProperty(BUCKET_PROP_KEY);

    Assertions.assertThrows(ServiceException.class, () -> {
      exportStorageService.removeFilesRelatedToJobExecution(null, null);
    });
  }

  @Test
  void testRemovingFilesFromS3ExportedFilesIsEmpty() {
    // when
    exportStorageService.removeFilesRelatedToJobExecution(new JobExecution(), TENANT_ID);

    // then
    Mockito.verify(minioClientFactory, never())
      .getClient();
  }

}
