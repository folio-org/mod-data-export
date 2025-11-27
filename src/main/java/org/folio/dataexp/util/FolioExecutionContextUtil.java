package org.folio.dataexp.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;

/** Utility class for working with FolioExecutionContext in a multi-tenant environment. */
@Log4j2
public class FolioExecutionContextUtil {

  private FolioExecutionContextUtil() {}

  /**
   * Prepares a FolioExecutionContext for the specified tenant.
   *
   * @param tenantId the tenant ID to set in the context
   * @param folioModuleMetadata the module metadata
   * @param context the current execution context
   * @return a new FolioExecutionContext for the given tenant
   * @throws IllegalStateException if Okapi headers are not provided
   */
  public static FolioExecutionContext prepareContextForTenant(
      String tenantId, FolioModuleMetadata folioModuleMetadata, FolioExecutionContext context) {
    if (MapUtils.isNotEmpty(context.getOkapiHeaders())) {
      // create deep copy of headers in order to make switching context thread safe
      var headersCopy =
          SerializationUtils.clone((HashMap<String, Collection<String>>) context.getAllHeaders());
      headersCopy.put(XOkapiHeaders.TENANT, List.of(tenantId));
      log.info("FOLIO context initialized with tenant {}", tenantId);
      return new DefaultFolioExecutionContext(folioModuleMetadata, headersCopy);
    }
    throw new IllegalStateException("Okapi headers not provided");
  }
}
