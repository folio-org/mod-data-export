package org.folio.dao.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.folio.dao.BucketDao;
import org.folio.rest.jaxrs.model.Bucket;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.interfaces.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import io.vertx.core.Future;

@Repository
public class BucketDaoImpl implements BucketDao {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String TABLE_NAME = "bucket";

  @Autowired
  private PostgresClientFactory pgClientFactory;

  @Override
  public Future<Optional<Bucket>> getByTenantId(String tenantId) {
    Future<Results<Bucket>> future = Future.future();
    try {
      pgClientFactory.getInstance(tenantId).get(TABLE_NAME, Bucket.class, new Criterion(), false, false, future.completer());
    } catch (Exception e) {
      LOGGER.error("Error getting S3 bucket for tenant", e);
      future.fail(e);
    }
    return future
      .map(Results::getResults)
      .map(r -> r.isEmpty() ? Optional.empty() : Optional.of(r.get(0)));
  }

  @Override
  public Future<String> save(String tenantId, Bucket tenantBucket) {
    Future<String> future = Future.future();
    if (StringUtils.isEmpty(tenantBucket.getId())) {
      tenantBucket.setId(UUID.randomUUID().toString());
    }
    pgClientFactory.getInstance(tenantId).save(TABLE_NAME, tenantBucket, future.completer());
    return future;
  }
}
