package org.folio.service.export.impl;

import java.lang.invoke.MethodHandles;

import org.folio.rest.jaxrs.model.Bucket;
import org.folio.service.export.AmazonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

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
}
