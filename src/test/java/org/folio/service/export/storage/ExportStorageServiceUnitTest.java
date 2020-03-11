package org.folio.service.export.storage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

import java.io.File;

import org.folio.rest.jaxrs.model.FileDefinition;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;

@RunWith(MockitoJUnitRunner.class)
public class ExportStorageServiceUnitTest {
  @Mock
  private AmazonFactory amazonFactory;
  @InjectMocks
  private ExportStorageService exportStorageService = new AWSStorageServiceImpl();
  private final static String TENANT_ID = "test_tenant";
  @After
  public void tearDown() {
    System.clearProperty("bucket.name");
  }

  public ExportStorageServiceUnitTest() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void storeFile_shouldPass() throws InterruptedException {
    // given
    System.setProperty("bucket.name", "TEST BUCKET");
    FileDefinition exportFileDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.txt");

    MultipleFileUpload multipleFileUploadMock = Mockito.mock(MultipleFileUpload.class);
    Mockito.doNothing().when(multipleFileUploadMock).waitForCompletion();

    TransferManager transferManagerMock = Mockito.mock(TransferManager.class);
    Mockito.when(transferManagerMock.uploadDirectory(anyString(), anyString(), any(File.class), anyBoolean()))
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
      .withSourcePath("files/mockData/generatedBinaryFile.txt");
    // when
    exportStorageService.storeFile(exportFileDefinition, TENANT_ID);
    // then expect RuntimeException
  }

  @Test(expected = RuntimeException.class)
  public void storeFile_shouldFailOnUploadDirectory() {
    // given
    System.setProperty("bucket.name", "TEST BUCKET");
    FileDefinition exportFileDefinition = new FileDefinition()
      .withSourcePath("files/mockData/generatedBinaryFile.txt");

    TransferManager transferManagerMock = Mockito.mock(TransferManager.class);
    Mockito.when(transferManagerMock.uploadDirectory(anyString(), anyString(), any(File.class), anyBoolean()))
      .thenThrow(new RuntimeException());
    Mockito.when(amazonFactory.getTransferManager()).thenReturn(transferManagerMock);

    // when
    exportStorageService.storeFile(exportFileDefinition, TENANT_ID);
    // then expect RuntimeException
  }
}
