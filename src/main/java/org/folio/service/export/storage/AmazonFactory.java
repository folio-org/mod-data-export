package org.folio.service.export.storage;

import org.springframework.stereotype.Component;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;

@Component
@Deprecated
public class AmazonFactory {

  public AmazonS3 getS3Client() {
    return AmazonS3ClientBuilder.standard().build();
  }

  public TransferManager getTransferManager() {
    return TransferManagerBuilder.standard().withS3Client(getS3Client()).build();
  }
}
