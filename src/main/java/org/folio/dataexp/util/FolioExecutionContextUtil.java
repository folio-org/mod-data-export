package org.folio.dataexp.util;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.utils.FolioExecutionContextUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

@Log4j2
public class FolioExecutionContextUtil {

  private FolioExecutionContextUtil(){}

  public static FolioExecutionContext prepareContextForTenant(String tenantId, FolioModuleMetadata folioModuleMetadata, FolioExecutionContext context) {
    return FolioExecutionContextUtils.prepareContextForTenant(tenantId, folioModuleMetadata, context);
  }
}
