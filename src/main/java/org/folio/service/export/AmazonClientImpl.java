package org.folio.service.export;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import org.folio.rest.jaxrs.model.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URL;

@Service
public class AmazonClientImpl implements AmazonClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public Bucket createS3Bucket(String name) throws SdkClientException {
    final AmazonS3 s3Client;
    final Regions clientRegion = Regions.DEFAULT_REGION;
    // This code expects that you have AWS credentials set up per:
    // https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html
    // uses credential Chain to fetch credentials
    s3Client = AmazonS3ClientBuilder.standard()
      .withRegion(clientRegion)
      .build();

    if (!s3Client.doesBucketExist(name))
      s3Client.createBucket(name);
    return new Bucket().withBucketName(name);
  }

  @Override
  public void copy(String bucketName) {
    AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
    TransferManager xfer_mgr = TransferManagerBuilder.standard()
      .withS3Client(s3Client)
      .build();
    try {
      System.err.println("uploading files");
      MultipleFileUpload xfer = xfer_mgr.uploadDirectory(bucketName, "kvMP",
        new File("/Users/kvuppala/git/folio-export-aws/fileExport"), false);

      xfer.waitForCompletion();
    } catch (AmazonServiceException e) {
      System.err.println(e.getErrorMessage());
      System.exit(1);
    } catch (AmazonClientException | InterruptedException e) {
      e.printStackTrace();
    }
    xfer_mgr.shutdownNow();
  }

  @Override
  public URL generatePresignedUrl(GeneratePresignedUrlRequest generatePresignedUrlRequest) {
    return null;
  }
}
