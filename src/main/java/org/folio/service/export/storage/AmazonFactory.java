package org.folio.service.export.storage;

import org.springframework.stereotype.Component;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;

@Component
public class AmazonFactory {
  private final static Regions region = Regions.DEFAULT_REGION;

  public AmazonS3 getS3Client() {
    return AmazonS3ClientBuilder.standard().withRegion(region).build();
  }

  public TransferManager getTransferManager() {
    AmazonS3 s3Client = getS3Client();
    return TransferManagerBuilder.standard().withS3Client(s3Client).build();
  }
}
