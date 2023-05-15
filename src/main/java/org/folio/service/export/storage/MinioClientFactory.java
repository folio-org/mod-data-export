package org.folio.service.export.storage;

import io.minio.MinioClient;
import io.minio.credentials.IamAwsProvider;
import io.minio.credentials.Provider;
import io.minio.credentials.StaticProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.s3.client.FolioS3Client;
import org.folio.s3.client.S3ClientFactory;
import org.folio.s3.client.S3ClientProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;

@Component
public class MinioClientFactory {

  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  @Value("${minio.endpoint}")
  private String endpoint;

  @Value("${minio.accessKey}")
  private String accessKey;

  @Value("${minio.secretKey}")
  private String secretKey;

  @Value("${minio.bucket}")
  private String bucket;

  @Value("${minio.region}")
  private String region;

  private MinioClient client;

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
      .awsSdk(false)
      .region(region)
      .build());
  }

  public MinioClient getClient() {

    if (client != null) {
      return client;
    }

    LOGGER.info("Creating MinIO client endpoint {},region {},bucket {},accessKey {},secretKey {}.", endpoint, region, bucket,
      StringUtils.isNotBlank(accessKey) ? "<set>" : "<not set>", StringUtils.isNotBlank(secretKey) ? "<set>" : "<not set>");

    var builder = MinioClient.builder().endpoint(endpoint);
    if (StringUtils.isNotBlank(region)) {
      builder.region(region);
    }

    Provider provider;
    if (StringUtils.isNotBlank(accessKey) && StringUtils.isNotBlank(secretKey)) {
      provider = new StaticProvider(accessKey, secretKey, null);
    } else {
      provider = new IamAwsProvider(null, null);
    }
    LOGGER.info("{} MinIO credentials provider created.", provider.getClass().getSimpleName());
    builder.credentialsProvider(provider);

    client = builder.build();

    return client;
  }
}
