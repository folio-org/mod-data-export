package org.folio.service.export.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.folio.config.ApplicationConfig;
import org.folio.rest.impl.RestVerticleTestBase;
import org.folio.spring.SpringContextUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.Context;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
class MinioClientFactoryTest extends RestVerticleTestBase {

  public MinioClientFactoryTest() {
    Context vertxContext = vertx.getOrCreateContext();
    SpringContextUtil.init(vertxContext.owner(), vertxContext, ApplicationConfig.class);
    SpringContextUtil.autowireDependencies(this, vertxContext);
  }

  @Autowired
  private MinioClientFactory minioClientFactory;

  @Test
  void getClientFromFactoryTest() {
    var client = minioClientFactory.getClient();
    assertNotNull(client);
    assertEquals(client, minioClientFactory.getClient());
  }
}
