package org.folio.service.export.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.UUID;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.rest.exceptions.HttpException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;

@RunWith(VertxUnitRunner.class)
public class ExportStorageServiceUnitTest {

  @Mock
  private AmazonFactory amazonFactory;
  @Spy
  private Vertx vertx = Vertx.vertx();
  @InjectMocks
  private ExportStorageService exportStorageService = new AWSStorageServiceImpl();

  private final static String TENANT_ID = "testTenant";
  private final static String BUCKET_NAME = "testBucket";

  public ExportStorageServiceUnitTest() {
    MockitoAnnotations.initMocks(this);
  }


  @Before
  public void setUp() {
    System.setProperty("bucket.name", BUCKET_NAME);
  }

  @After
  public void tearDown() {
    System.clearProperty("bucket.name");
  }

  @Test
  public void storeFile_shouldPass() throws InterruptedException {
    // given
    String jobId = UUID.randomUUID().toString();
    String parentFolder = TENANT_ID + "/" + jobId;

    FileDefinition exportFileDefinition = new FileDefinition()
      .withJobExecutionId(jobId)
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    File file = Paths.get(exportFileDefinition.getSourcePath()).getFileName().toFile();

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

  @Test(expected = IllegalStateException.class)
  public void storeFile_shouldFailIfBucketNameIsNotSet() {
    // given
    System.clearProperty("bucket.name");
    FileDefinition exportFileDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    // when
    exportStorageService.storeFile(exportFileDefinition, TENANT_ID);
    // then expect RuntimeException
  }

  @Test(expected = RuntimeException.class)
  public void storeFile_shouldFailOnUploadDirectory() {
    // given
    FileDefinition exportFileDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.mrc");
    TransferManager transferManagerMock = Mockito.mock(TransferManager.class);
    Mockito.when(transferManagerMock.uploadDirectory(anyString(), anyString(), any(File.class), anyBoolean()))
      .thenThrow(new RuntimeException());
    Mockito.when(amazonFactory.getTransferManager()).thenReturn(transferManagerMock);
    // when
    exportStorageService.storeFile(exportFileDefinition, TENANT_ID);
    // then expect RuntimeException
  }

  @Test
  public void testSuccessfulGenerateURL(TestContext testContext) throws MalformedURLException {
    Async async = testContext.async();
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
    linkFuture.setHandler(ar -> {
      assertTrue(ar.succeeded());
      assertEquals(response.toString(), ar.result());
      async.complete();
    });
  }

  @Test
  public void testbucketNameNotFoundInS3(TestContext testContext) {
    Async async = testContext.async();
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
    linkFuture.setHandler(ar -> {
      assertTrue(ar.failed());
      assertEquals("Bucket Not Found", ar.cause().getMessage());
      async.complete();
    });
  }

  @Test(expected = HttpException.class)
  public void testbucketNameNotProvidedInSystemProperty() {
    System.clearProperty("bucket.name");

    AmazonS3 s3ClientMock = Mockito.mock(AmazonS3.class);
    when(amazonFactory.getS3Client()).thenReturn(s3ClientMock);

    exportStorageService.getFileDownloadLink(null, null, null);
  }
}
