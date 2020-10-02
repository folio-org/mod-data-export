package org.folio.service.export.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.ErrorLog;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.logs.ErrorLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class ExportStorageServiceUnitTest {

  @Mock
  private AmazonFactory amazonFactory;
  @Mock
  private ErrorLogService errorLogService;
  @Spy
  private Vertx vertx = Vertx.vertx();
  @InjectMocks
  private ExportStorageService exportStorageService = new AWSStorageServiceImpl();

  @Captor
  private ArgumentCaptor<ErrorLog> errorLogCaptor;

  private final static String TENANT_ID = "testTenant";
  private final static String BUCKET_NAME = "testBucket";


  @BeforeEach
  void setUp() {
    System.setProperty("bucket.name", BUCKET_NAME);
  }

  @AfterEach
  void tearDown() {
    System.clearProperty("bucket.name");
  }

  @Test
  void storeFile_shouldPass() throws InterruptedException {
    // given
    String jobId = UUID.randomUUID().toString();
    String parentFolder = TENANT_ID + "/" + jobId;

    FileDefinition exportFileDefinition = new FileDefinition()
      .withJobExecutionId(jobId)
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    File file = Paths.get(exportFileDefinition.getSourcePath()).getParent().toFile();

    MultipleFileUpload multipleFileUploadMock = Mockito.mock(MultipleFileUpload.class);
    Mockito.doNothing().when(multipleFileUploadMock).waitForCompletion();

    TransferManager transferManagerMock = Mockito.mock(TransferManager.class);
    Mockito.when(transferManagerMock.uploadDirectory(BUCKET_NAME, parentFolder, file, false))
      .thenReturn(multipleFileUploadMock);
    Mockito.when(amazonFactory.getTransferManager()).thenReturn(transferManagerMock);

    Mockito.doNothing().when(transferManagerMock).shutdownNow();
    // when
    exportStorageService.storeFile(exportFileDefinition, TENANT_ID);
    // then
    Mockito
      .verify(transferManagerMock, Mockito.times(1))
      .uploadDirectory(anyString(), anyString(), any(File.class), anyBoolean());
    Mockito
      .verify(transferManagerMock, Mockito.times(1))
      .shutdownNow();
    Mockito
      .verify(multipleFileUploadMock, Mockito.times(1))
      .waitForCompletion();
  }

  @Test
  void storeFile_shouldFailIfBucketNameIsNotSet() {
    // given
    System.clearProperty("bucket.name");
    FileDefinition exportFileDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    // when

    Assertions.assertThrows(ServiceException.class, () -> {
      exportStorageService.storeFile(exportFileDefinition, TENANT_ID);
    });
    // then expect RuntimeException
  }

  @Test
  void storeFile_shouldFailOnUploadDirectory() {
    // given
    FileDefinition exportFileDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    TransferManager transferManagerMock = Mockito.mock(TransferManager.class);
    Mockito.when(transferManagerMock.uploadDirectory(anyString(), anyString(), any(File.class), anyBoolean()))
      .thenThrow(new RuntimeException());
    Mockito.when(amazonFactory.getTransferManager()).thenReturn(transferManagerMock);
    // when

    // then expect RuntimeException
    Assertions.assertThrows(RuntimeException.class, () -> {
      exportStorageService.storeFile(exportFileDefinition, TENANT_ID);
    });
  }

  @Test
  void testSuccessfulGenerateURL(VertxTestContext testContext) throws MalformedURLException {
    // given
    String tenantId = "testAWS";
    String jobExecutionId = "67dfac11-1caf-4470-9ad1-d533f6360bdd";
    String fileId = "448ae575-daec-49c1-8041-d64c8ed8e5b1";

    AmazonS3 s3ClientMock = Mockito.mock(AmazonS3.class);
    when(amazonFactory.getS3Client()).thenReturn(s3ClientMock);

    URL response = new URL("https://test-aws-export-vk.s3.amazonaws.com");
    when(s3ClientMock.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(response);
    // when
    Future<String> linkFuture = exportStorageService.getFileDownloadLink(jobExecutionId, fileId, tenantId);
    // then
    linkFuture.onComplete(ar -> {
      testContext.verify(()-> {
        assertTrue(ar.succeeded());
        assertEquals(response.toString(), ar.result());
        verify(s3ClientMock).shutdown();
        testContext.completeNow();
      });

    });
  }

  @Test
  void testbucketNameNotFoundInS3(VertxTestContext testContext) {
    // given
    String jobExecutionId = "67dfac11-1caf-4470-9ad1-d533f6360bdd";
    String fileId = "448ae575-daec-49c1-8041-d64c8ed8e5b1";

    AmazonS3 s3ClientMock = Mockito.mock(AmazonS3.class);
    when(amazonFactory.getS3Client()).thenReturn(s3ClientMock);

    doThrow(new SdkClientException("Bucket Not Found")).when(s3ClientMock)
      .generatePresignedUrl(any(GeneratePresignedUrlRequest.class));
    // when
    Future<String> linkFuture = exportStorageService.getFileDownloadLink(jobExecutionId, fileId, TENANT_ID);
    // then
    linkFuture.onComplete(ar -> {
      testContext.verify(() -> {
        assertTrue(ar.failed());
        assertEquals("Bucket Not Found", ar.cause().getMessage());
        verify(s3ClientMock).shutdown();
        testContext.completeNow();
      });

    });
  }

  @Test
  void testbucketNameNotProvidedInSystemProperty() {
    System.clearProperty("bucket.name");

    AmazonS3 s3ClientMock = Mockito.mock(AmazonS3.class);
    when(amazonFactory.getS3Client()).thenReturn(s3ClientMock);

    Assertions.assertThrows(ServiceException.class, () -> {
      exportStorageService.getFileDownloadLink(null, null, null);
    });

  }
}
