package org.folio.service.storage.aws.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.folio.dao.BucketDao;
import org.folio.rest.jaxrs.model.Bucket;
import org.folio.service.storage.aws.AwsService;
import org.folio.service.storage.aws.FolioAwsClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.SdkClientException;

import io.vertx.core.Future;

@RunWith(MockitoJUnitRunner.class)
public class AwsServiceTest {

  private final String TENANT_ID = "test_tenant";

  @Mock
  private BucketDao bucketDao;
  @Mock
  private FolioAwsClient folioAwsClient;
  private AwsService awsService;
  private Bucket bucket;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    bucket = new Bucket();
    bucket.setBucketName(TENANT_ID);
    awsService = new AwsServiceImpl(bucketDao, folioAwsClient);
  }

  @Test
  public void testBucketIsCreatedForATenant() {
    when(folioAwsClient.createS3Bucket(anyString())).thenReturn(bucket);
    when(bucketDao.getByTenantId(anyString())).thenReturn(Future.succeededFuture(Optional.empty()));

    final Future<String> result = awsService.setUpS33BucketForTenant(TENANT_ID);
    assertFalse(result.failed());

    verify(bucketDao).save(eq(TENANT_ID), any(Bucket.class));
  }


  @Test
  public void testBucketIsNotCreatedIfItAlreadyExists() {
    Bucket mockBucket = new Bucket();
    mockBucket.setBucketName(TENANT_ID);
    when(bucketDao.getByTenantId(anyString())).thenReturn(Future.succeededFuture(Optional.of(mockBucket)));

    final Future<String> result = awsService.setUpS33BucketForTenant(TENANT_ID);
    assertFalse(result.failed());
    verify(folioAwsClient, never()).createS3Bucket(anyString());
    verify(bucketDao, never()).save(eq(TENANT_ID), any(Bucket.class));

  }

  @Test
  public void setUpS33BucketForTenantReturnsFailedFutureOnAwsSdkException() {
    when(bucketDao.getByTenantId(anyString())).thenReturn(Future.succeededFuture(Optional.empty()));
    when(folioAwsClient.createS3Bucket(anyString())).thenThrow(new SdkClientException("test"));

    final Future<String> result = awsService.setUpS33BucketForTenant(TENANT_ID);

    verify(folioAwsClient).createS3Bucket(anyString());
    verify(bucketDao, never()).save(eq(TENANT_ID), any(Bucket.class));

    assertTrue(result.failed());
    assert result.cause() instanceof SdkClientException;
  }
}
