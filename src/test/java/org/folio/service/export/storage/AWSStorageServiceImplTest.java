package org.folio.service.export.storage;

import static com.amazonaws.SDKGlobalConfiguration.ACCESS_KEY_SYSTEM_PROPERTY;
import static com.amazonaws.SDKGlobalConfiguration.SECRET_KEY_SYSTEM_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@RunWith(VertxUnitRunner.class)
public class AWSStorageServiceImplTest {

  @Mock
  AWSClient awsClient;

  AWSStorageServiceImpl awsStorageService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    System.setProperty(ACCESS_KEY_SYSTEM_PROPERTY, "bogus");
    System.setProperty(SECRET_KEY_SYSTEM_PROPERTY, "bogus");
    System.setProperty("region", "bogus");
    awsStorageService = new AWSStorageServiceImpl();
  }

  @Test
  public void testSuccessfulGenerateURL() throws MalformedURLException {
    String bucketName = "testBucket";
    System.setProperty("bucket.name", bucketName);
    String tenantId = "testAWS";
    String jobExecutionId = "67dfac11-1caf-4470-9ad1-d533f6360bdd";
    String fileId = "448ae575-daec-49c1-8041-d64c8ed8e5b1";

    String keyName = tenantId + "/" + jobExecutionId + "/" + fileId + ".mrc";

    GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, keyName)
      .withMethod(HttpMethod.GET)
      .withExpiration(new Date());
    AmazonS3 s3Client = Mockito.mock(AmazonS3.class);

    when(awsClient.getAWSS3Client()).thenReturn(s3Client);

    URL response = new URL(
        "https://test-aws-export-vk.s3.amazonaws.com/CatShip.mrc?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20200220T185104Z&X-Amz-SignedHeaders=host&X-Amz-Expires=3599&X-Amz-Credential=AKUAIZOH6UABI4QDF678%2F20200220%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Signature=2d87fd96cd1b05ee74228ee9124bc0ad34196a08b03c62453e9588aa3e485a77\"");
    when(s3Client.generatePresignedUrl(generatePresignedUrlRequest)).thenReturn(response);

    assertEquals(response.toString(), awsStorageService.getFileDownloadLink(jobExecutionId, fileId, tenantId));

  }

}
