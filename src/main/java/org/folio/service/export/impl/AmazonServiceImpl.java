package org.folio.service.export.impl;

import java.lang.invoke.MethodHandles;
import java.util.UUID;

import org.folio.dao.BucketDao;
import org.folio.rest.jaxrs.model.Bucket;
import org.folio.service.export.AmazonService;
import org.folio.service.export.AmazonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.SdkClientException;

import io.vertx.core.Future;
import io.vertx.core.Promise;

@Service
public class AmazonServiceImpl implements AmazonService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String BUCKET_NAME_SUFFIX = "export";
  private final AmazonClient amazonClient;
  private final BucketDao bucketDao;

  public AmazonServiceImpl(@Autowired BucketDao bucketDao, @Autowired AmazonClient amazonClient) {
    this.bucketDao = bucketDao;
    this.amazonClient = amazonClient;
  }

  @Override
  public Future<String> setUpS33BucketForTenant(String tenantId) {
    Promise<String> promise = Promise.promise();
    bucketDao.getByTenantId(tenantId).map(o -> {
      if (!o.isPresent()) {
        final String bucketName = generateBucketName(tenantId);
        LOGGER.info("Creating S3 bucket for tenant {}", bucketName);
        final Bucket s3Bucket;
        try {
          s3Bucket = amazonClient.createS3Bucket(bucketName);
          bucketDao.save(tenantId, s3Bucket);
          promise.complete(s3Bucket.getBucketName());
        } catch (SdkClientException e) {
          promise.fail(e);
        }
      }
      return promise;
    });
    return promise.future();
  }

  private static String generateBucketName(String tenantName) {
    return String.format("%s-%s-%s", tenantName, BUCKET_NAME_SUFFIX, UUID.randomUUID().toString());
  }
}
