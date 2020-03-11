package org.folio.service.export.storage;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import java.net.MalformedURLException;
import java.net.URL;
import org.folio.rest.exceptions.HttpException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AWSStorageServiceImplTest {

  @Mock
  AWSClient awsClient;

  @InjectMocks
  ExportStorageService awsStorageService = new AWSStorageServiceImpl();;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    String bucketName = "testBucket";
    System.setProperty("bucket.name", bucketName);
  }

  @Test
  public void testSuccessfulGenerateURL() throws MalformedURLException {
    String tenantId = "testAWS";
    String jobExecutionId = "67dfac11-1caf-4470-9ad1-d533f6360bdd";
    String fileId = "448ae575-daec-49c1-8041-d64c8ed8e5b1";

    AmazonS3 s3ClientMock = Mockito.mock(AmazonS3.class);

    when(awsClient.getAWSS3Client()).thenReturn(s3ClientMock);

    URL response = new URL("https://test-aws-export-vk.s3.amazonaws.com");
    when(s3ClientMock.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(response);

    assertEquals(response.toString(), awsStorageService.getFileDownloadLink(jobExecutionId, fileId, tenantId));

  }

  @Test(expected = SdkClientException.class)
  public void testbucketNameNotFoundInS3() {

    String tenantId = "testAWS";
    String jobExecutionId = "67dfac11-1caf-4470-9ad1-d533f6360bdd";
    String fileId = "448ae575-daec-49c1-8041-d64c8ed8e5b1";

    AmazonS3 s3ClientMock = Mockito.mock(AmazonS3.class);
    when(awsClient.getAWSS3Client()).thenReturn(s3ClientMock);

    doThrow(new SdkClientException("Bucket Not Found")).when(s3ClientMock)
      .generatePresignedUrl(any(GeneratePresignedUrlRequest.class));

    awsStorageService.getFileDownloadLink(jobExecutionId, fileId, tenantId);

  }

  @Test(expected = HttpException.class)
  public void testbucketNameNotProvidedInSystemProperty() {
    System.clearProperty("bucket.name");

    AmazonS3 s3ClientMock = Mockito.mock(AmazonS3.class);
    when(awsClient.getAWSS3Client()).thenReturn(s3ClientMock);

    awsStorageService.getFileDownloadLink(null, null, null);

  }

}
