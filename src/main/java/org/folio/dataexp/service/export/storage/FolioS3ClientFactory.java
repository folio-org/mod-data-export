package org.folio.dataexp.service.export.storage;

import org.folio.s3.client.FolioS3Client;
import org.folio.s3.client.S3ClientFactory;
import org.folio.s3.client.S3ClientProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FolioS3ClientFactory {

  @Value("${application.remote-files-storage.endpoint}")
  private String endpoint;

  @Value("${application.remote-files-storage.accessKey}")
  private String accessKey;

  @Value("${application.remote-files-storage.secretKey}")
  private String secretKey;

  @Value("${application.remote-files-storage.bucket}")
  private String bucket;

  @Value("${application.remote-files-storage.region}")
  private String region;

  @Value("#{ T(Boolean).parseBoolean('${application.remote-files-storage.awsSdk}')}")
  private boolean awsSdk;

  private FolioS3Client folioS3Client; //NOSONAR

  public FolioS3Client getFolioS3Client() {
    if (folioS3Client != null) {
      return folioS3Client;
    }
    folioS3Client = createFolioS3Client();
    return folioS3Client;
  }

  private FolioS3Client createFolioS3Client() {
    return S3ClientFactory.getS3Client(S3ClientProperties.builder()
      .endpoint(endpoint)
      .secretKey(secretKey)
      .accessKey(accessKey)
      .bucket(bucket)
      .awsSdk(awsSdk)
      .region(region)
      .build());
  }
}
