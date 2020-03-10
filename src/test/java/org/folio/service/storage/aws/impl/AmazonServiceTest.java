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
import org.folio.service.export.AmazonClient;
import org.folio.service.export.AmazonService;
import org.folio.service.export.impl.AmazonServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.SdkClientException;

import io.vertx.core.Future;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class AmazonServiceTest {

  private final String TENANT_ID = "test_tenant";

  @Mock
  private BucketDao bucketDao;
  @Mock
  private AmazonClient amazonClient;
  private AmazonService amazonService;
  private Bucket bucket;

  @Before
  public void setUp(TestContext testContext) {
    MockitoAnnotations.initMocks(this);
    bucket = new Bucket();
    bucket.setBucketName(TENANT_ID);
    amazonService = new AmazonServiceImpl(bucketDao, amazonClient);
  }

  @Test
  public void testBucketIsCreatedForATenant(TestContext testContext) {
    final Async async = testContext.async();
    //given
    when(amazonClient.createS3Bucket(anyString())).thenReturn(bucket);
    when(bucketDao.getByTenantId(anyString())).thenReturn(Future.succeededFuture(Optional.empty()));

    //when
    final Future<String> future = amazonService.setUpS33BucketForTenant(TENANT_ID);

    //then
    future.setHandler(ar -> {
      assertFalse(ar.failed());
      verify(bucketDao).save(eq(TENANT_ID), any(Bucket.class));

      async.complete();
    });

  }

  @Test
  public void testBucketIsNotCreatedIfItAlreadyExists(TestContext testContext) {
    final Async async = testContext.async();
    //given
    Bucket mockBucket = new Bucket();
    mockBucket.setBucketName(TENANT_ID);
    when(bucketDao.getByTenantId(anyString())).thenReturn(Future.succeededFuture(Optional.of(mockBucket)));

    //when
    final Future<String> future = amazonService.setUpS33BucketForTenant(TENANT_ID);

    //then
    assertFalse(future.failed());
    verify(amazonClient, never()).createS3Bucket(anyString());
    verify(bucketDao, never()).save(eq(TENANT_ID), any(Bucket.class));

    async.complete();

  }

  @Test
  public void setUpS33BucketForTenantReturnsFailedFutureOnAwsSdkException(TestContext testContext) {
    final Async async = testContext.async();
    //given
    when(bucketDao.getByTenantId(anyString())).thenReturn(Future.succeededFuture(Optional.empty()));
    when(amazonClient.createS3Bucket(anyString())).thenThrow(new SdkClientException("test"));

    //when
    final Future<String> future = amazonService.setUpS33BucketForTenant(TENANT_ID);

    //then
    future.setHandler(ar -> {
      assertTrue(ar.failed());
      assert ar.cause() instanceof SdkClientException;
      async.complete();
    });
  }
}
