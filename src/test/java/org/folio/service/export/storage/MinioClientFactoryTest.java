package org.folio.service.export.storage;

import static org.folio.service.export.storage.MinioClientFactory.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class MinioClientFactoryTest {

  @Test
  void getClientStaticProviderTest() {
    System.setProperty(ACCESS_KEY_PROP_KEY, "test-access-key");
    System.setProperty(ENDPOINT_PROP_KEY, "http://localhost:21345");
    System.setProperty(REGION_PROP_KEY, "test-region");
    System.setProperty(BUCKET_PROP_KEY, "test-bucket");
    System.setProperty(SECRET_KEY_PROP_KEY, "test-secret-key");

    var factory = new MinioClientFactory();
    var client = factory.getClient();
    assertNotNull(client);
    assertEquals(client, factory.getClient());
  }

  @Test
  void getClientIamProviderTest() {
    System.clearProperty(ACCESS_KEY_PROP_KEY);
    System.clearProperty(SECRET_KEY_PROP_KEY);
    System.setProperty(ENDPOINT_PROP_KEY, "http://localhost:21345");
    System.setProperty(REGION_PROP_KEY, "test-region");
    System.setProperty(BUCKET_PROP_KEY, "test-bucket");

    var factory = new MinioClientFactory();
    var client = factory.getClient();
    assertNotNull(client);
    assertEquals(client, factory.getClient());
  }
}
