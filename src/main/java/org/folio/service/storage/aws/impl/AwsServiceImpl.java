package org.folio.service.storage.aws.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.UUID;

import org.folio.dao.BucketDao;
import org.folio.rest.jaxrs.model.Bucket;
import org.folio.service.storage.aws.AwsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import io.vertx.core.Future;

@Service
public class AwsServiceImpl implements AwsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String BUCKET_NAME_SUFFIX = "export";

  @Autowired
  private BucketDao bucketDao;

  @Override
  public Future<Void> setUpS33BucketForTenant(String tenantId) {
    return bucketDao.getByTenantId(tenantId).map(o -> {
      if (!o.isPresent()) {
        createS3Bucket(tenantId).ifPresent(bucket -> bucketDao.save(tenantId, bucket));
      }
      return null;
    });
  }

  private Optional<Bucket> createS3Bucket(String tenantId) {
    final AmazonS3 s3Client;
    final Regions clientRegion = Regions.DEFAULT_REGION;
    String bucketName = generateBucketName(tenantId);
    try {
      // This code expects that you have AWS credentials set up per:
      // https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html
      // uses credential Chain to fetch credentials
      s3Client = AmazonS3ClientBuilder.standard()
        .withRegion(clientRegion)
        .build();

      if (!s3Client.doesBucketExist(bucketName)) {
        LOGGER.info("Creating bucket {} for tenant: {}", bucketName, tenantId);
        s3Client.createBucket(bucketName);
        final Bucket tenantBucket = new Bucket().withBucketName(bucketName);
        return Optional.of(tenantBucket);
      }
    } catch (SdkClientException e) {
      LOGGER.error("Exception while creating S3 bucket", e);
    }
    return Optional.empty();
  }

  private static String generateBucketName(String tenantName) {
    return String.format("%s-%s-%s", tenantName, BUCKET_NAME_SUFFIX, UUID.randomUUID().toString());
  }
}
