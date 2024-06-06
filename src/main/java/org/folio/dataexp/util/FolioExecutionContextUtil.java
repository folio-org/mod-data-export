package org.folio.dataexp.util;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.folio.okapi.common.XOkapiHeaders;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

@Log4j2
public class FolioExecutionContextUtil {

  public static FolioExecutionContext prepareContextForTenant(String tenantId, FolioModuleMetadata folioModuleMetadata, FolioExecutionContext context) {
    if (MapUtils.isNotEmpty(context.getOkapiHeaders())) {
      // create deep copy of headers in order to make switching context thread safe
      var headersCopy = SerializationUtils.clone((HashMap<String, Collection<String>>) context.getAllHeaders());
      headersCopy.put(XOkapiHeaders.TENANT, List.of(tenantId));
      log.info("FOLIO context initialized with tenant {}", tenantId);
      return new DefaultFolioExecutionContext(folioModuleMetadata, headersCopy);
    }
    throw new IllegalStateException("Okapi headers not provided");
  }

  public static void runInFolioContext(FolioExecutionContext context, Runnable runnable) {
    try (var fec = new FolioExecutionContextSetter(context)) {
      runnable.run();
    }
  }
}
