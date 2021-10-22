package org.folio.rest.impl.tenantapi;

import io.restassured.http.Header;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.impl.StorageTestSuite;
import org.folio.rest.impl.tenantapi.util.TenantReferenceApiTestUtil;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.jaxrs.model.TenantJob;
import org.folio.rest.persist.PostgresClient;
import org.folio.util.TestEntities;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.impl.tenantapi.util.TenantReferenceApiTestUtil.deleteTenant;
import static org.folio.rest.impl.tenantapi.util.TenantReferenceApiTestUtil.postTenant;
import static org.folio.rest.impl.tenantapi.util.TenantReferenceApiTestUtil.purge;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

public class TenantReferenceApiTest extends TenantReferenceApiTestBase {

  private final Logger logger = LogManager.getLogger(TenantReferenceApiTest.class);

  private static final String TEST_EXPECTED_QUANTITY_FOR_ENTRY = "Test expected {} quantity for {}";

  private static final Header NONEXISTENT_TENANT_HEADER = new Header(OKAPI_HEADER_TENANT, "no_tenant");
  private static final Header PARTIAL_TENANT_HEADER = new Header(OKAPI_HEADER_TENANT, "partial_tenant");
  private static TenantJob tenantJob;

  @Test
  void testLoadReferenceData() throws MalformedURLException {
    logger.info("load only Reference Data");
    try {
      TenantAttributes tenantAttributes = TenantReferenceApiTestUtil.prepareTenantBody(false, true);
      tenantJob = postTenant(PARTIAL_TENANT_HEADER, tenantAttributes);

      verifyCollectionQuantity("/data-export/job-profiles", 2, PARTIAL_TENANT_HEADER);
      for (TestEntities entity : TestEntities.values()) {
        if (!entity.equals(TestEntities.MAPPINGPROFILE)) {
          logger.info("Test sample data not loaded for " + entity.name() + " because sample data is not implemented");
          verifyCollectionQuantity(entity.getEndpoint(), 2, PARTIAL_TENANT_HEADER);
        }
      }
    } finally {
      PostgresClient oldClient = PostgresClient.getInstance(StorageTestSuite.getVertx(), PARTIAL_TENANT_HEADER.getValue());
      deleteTenant(tenantJob, PARTIAL_TENANT_HEADER);
      PostgresClient newClient = PostgresClient.getInstance(StorageTestSuite.getVertx(), PARTIAL_TENANT_HEADER.getValue());
      assertThat(oldClient, not(newClient));
    }
  }

  @Test
  void upgradeTenantWithNonExistentDb() throws MalformedURLException {
    logger.info("upgrading Module for non existed tenant");
    TenantAttributes tenantAttributes = TenantReferenceApiTestUtil.prepareTenantBody(false, false);
    try {
      postTenant(NONEXISTENT_TENANT_HEADER, tenantAttributes);

      // Check that no sample data loaded
      for (TestEntities entity : TestEntities.values()) {
        logger.info(TEST_EXPECTED_QUANTITY_FOR_ENTRY, 0, entity.name());
        verifyCollectionQuantity(entity.getEndpoint(), 0, NONEXISTENT_TENANT_HEADER);
      }
    }
    finally {
      purge(NONEXISTENT_TENANT_HEADER);
    }
  }
}
