package org.folio.service.storage.aws;

import io.vertx.core.Future;

/**
 * Utility service to interact with AWS
 */
public interface AwsService {

  /**
   * Create a bucket in S3
   *
   * @param tenantId - tenant id is used in a part of the name for the bucket to be created
   */
  Future<Void> setUpS33BucketForTenant(String tenantId);
}
