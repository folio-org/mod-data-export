package org.folio.service.export.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.vertx.core.Vertx;
import org.folio.spring.SpringContextUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.Context;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
class MinioClientFactoryTest {

  @Spy
  private Vertx vertx = Vertx.vertx();

  public MinioClientFactoryTest() {
    Context vertxContext = vertx.getOrCreateContext();
    SpringContextUtil.init(vertxContext.owner(), vertxContext, ApplicationConfig.class);
    SpringContextUtil.autowireDependencies(this, vertxContext);
  }

  @Autowired
  private FolioS3ClientFactory folioS3ClientFactory;

  @Test
  void getFolioS3ClientFromFactoryTest() {
    var folioS3Client = folioS3ClientFactory.getFolioS3Client();
    assertNotNull(folioS3Client);
    assertEquals(folioS3Client, folioS3ClientFactory.getFolioS3Client());
  }
}
