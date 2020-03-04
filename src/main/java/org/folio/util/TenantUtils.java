package org.folio.util;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.folio.rest.jaxrs.model.TenantAttributes;

import java.util.UUID;

public class TenantUtils {

  private static AmazonS3 s3Client;
  private static Regions clientRegion = Regions.US_EAST_1;

  private static String stringObjKeyName = "testFile1.mrc";
  private static String keyName = "testFileMultiPart.mrc";

  public static void main(String[] args) {

  }


  public static void setUpS3Bucket(TenantAttributes tenantAttributes){


    String bucketName =

      ;try {
      // This code expects that you have AWS credentials set up per:
      // https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html
      // uses credential Chain to fetch credentials
      s3Client = AmazonS3ClientBuilder.standard()
        .withRegion(clientRegion)
        .build();

      if (!s3Client.doesBucketExist(bucketName)) {
        s3Client.createBucket(bucketName);
      }

    } catch (SdkClientException e) {
      // The call was transmitted successfully, but Amazon S3 couldn't process
      // it, so it returned an error response.
      e.printStackTrace();
    }// Amazon S3 couldn't be contacted for a response, or the client
// couldn't parse the response from Amazon S3.

  }

  /**
   * Creates a file with the given String
   */
  public static void uploadString() {
    // Upload a text string as a new object.
    s3Client.putObject(bucketName, stringObjKeyName, "Uploaded String Object123");

  }


  private String generateBucketName(String tenantName) {
    return String.format("%s-%s-%s", tenantName, "export", UUID.randomUUID().toString());
  }

}
