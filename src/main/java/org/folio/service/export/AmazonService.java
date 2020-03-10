package org.folio.service.export;

import io.vertx.core.Future;

/**
 * Utility service to interact with AWS
 */
public interface AmazonService {

  /**
   * Create a bucket in S3. Each tenant will have its own bucket created by the system.
   *
   * @param tenantId - tenant id is used in a part of the name for the bucket to be created: $tenantId-export-$randomUUID
   * @return Succeeded future with newly created bucket name. Returns failed future in case of any errors in bucket creation in AWS.
   */
  Future<String> setUpS33BucketForTenant(String tenantId);
}
