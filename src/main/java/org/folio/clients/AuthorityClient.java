package org.folio.clients;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.service.logs.ErrorLogService;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.folio.util.ExternalPathResolver.AUTHORITY;
import static org.folio.util.ExternalPathResolver.resourcesPathWithPrefix;

@Component
public class AuthorityClient {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());
  private static final String QUERY_PATTERN_WITH_SOURCE = "id==%s and source==";
  private static final String QUERY_LIMIT_PATTERN = "?query=(%s)&limit=";


  @Autowired
  private ErrorLogService errorLogService;

  public Optional<JsonObject> getAuthoritiesByIds(List<String> ids, String jobExecutionId, OkapiConnectionParams params, String source) {
    try {
      return Optional.of(ClientUtil.getByIds(ids, params, resourcesPathWithPrefix(AUTHORITY) + QUERY_LIMIT_PATTERN + ids.size(),
        "(" + QUERY_PATTERN_WITH_SOURCE + source + ")"));
    } catch (HttpClientException exception) {
      LOGGER.error(exception.getMessage(), exception.getCause());
      errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.ERROR_GETTING_INSTANCES_BY_IDS.getCode(), Arrays.asList(exception.getMessage()), jobExecutionId, params.getTenantId());
      return Optional.empty();
    }
  }
}
