package org.folio.service.export.storage;

import io.minio.MinioClient;
import io.minio.credentials.IamAwsProvider;
import io.minio.credentials.Provider;
import io.minio.credentials.StaticProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;

import static java.lang.System.getProperty;

@Component
public class ClientFactory {

  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  public static final String ACCESS_KEY_PROP_KEY = "AWS_ACCESS_KEY_ID";
  public static final String ENDPOINT_PROP_KEY = "AWS_URL";
  public static final String REGION_PROP_KEY = "AWS_REGION";
  public static final String SECRET_KEY_PROP_KEY = "AWS_SECRET_ACCESS_KEY";
  public static final String BUCKET_PROP_KEY = "AWS_BUCKET";

  private MinioClient client;

  public MinioClient getClient() {

    if (client != null) {
      return client;
    }

    final String accessKey = getProperty(ACCESS_KEY_PROP_KEY);;
    final String endpoint = getProperty(ENDPOINT_PROP_KEY);
    final String region = getProperty(REGION_PROP_KEY);
    final String bucket = getProperty(BUCKET_PROP_KEY);
    final String secretKey = getProperty(SECRET_KEY_PROP_KEY);

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
