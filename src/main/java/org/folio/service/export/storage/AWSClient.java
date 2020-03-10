package org.folio.service.export.storage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.stereotype.Service;

@Service
public class AWSClient {

  public AmazonS3 getAWSS3Client() {
    return AmazonS3ClientBuilder.defaultClient();
  }

}
