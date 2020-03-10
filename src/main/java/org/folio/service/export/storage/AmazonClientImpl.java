package org.folio.service.export.storage;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URL;

import org.folio.rest.jaxrs.model.FileDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;

@Service
public class AmazonClientImpl implements AmazonClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final static Regions region = Regions.DEFAULT_REGION;

  @Override
  public void storeFile(String bucketName, FileDefinition fileDefinition) {
    AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
      .withRegion(region)
      .build();
    TransferManager xfer_mgr = TransferManagerBuilder.standard()
      .withS3Client(s3Client)
      .build();
    try {
      LOGGER.info("Uploading files to {}", bucketName);
      MultipleFileUpload xfer = xfer_mgr.uploadDirectory(bucketName, "data-export",
        new File(fileDefinition.getSourcePath()), false);

      xfer.waitForCompletion();
    } catch (AmazonServiceException e) {
      LOGGER.error(e.getErrorMessage());
    } catch (InterruptedException e) {
      LOGGER.error(e.getMessage());
    }
    xfer_mgr.shutdownNow();
  }

  @Override
  public URL generatePresignedUrl(GeneratePresignedUrlRequest generatePresignedUrlRequest) {
    return AmazonS3ClientBuilder.defaultClient().generatePresignedUrl(generatePresignedUrlRequest);
  }
}
