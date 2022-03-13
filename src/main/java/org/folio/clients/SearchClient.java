package org.folio.clients;

import static java.lang.String.format;
import static java.util.Objects.nonNull;

import static org.apache.logging.log4j.util.Strings.EMPTY;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.util.ExternalPathResolver.SEARCH_IDS;
import static org.folio.util.ExternalPathResolver.resourcesPathWithPrefix;

import com.fasterxml.jackson.core.JsonParseException;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.JsonParser;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.service.logs.ErrorLogService;
import org.folio.util.ErrorCode;
import org.folio.util.OkapiConnectionParams;
import org.folio.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MediaType;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Optional;

@Component
public class SearchClient {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());
  private static final String QUERY = "?query=";
  private static final String IDS_LIST = "ids";
  private static final String ERROR_MESSAGE_INVALID_STATUS_CODE = "Exception while calling %s, message: Get invalid response with status: %s";
  private static final String ERROR_MESSAGE_INVALID_BODY = "Exception while calling %s, message: Got invalid response body: %s";
  private static final String ERROR_MESSAGE_EMPTY_BODY = "Exception while calling %s, message: empty body returned.";
  private static final String ERROR_MESSAGE_NO_RECORDS = "Exception while calling %s, message: No UUIDs were found for the query.";

  private final WebClient webClient;
  private final ErrorLogService errorLogService;

  @Autowired
  public SearchClient(WebClient webClient, ErrorLogService errorLogService) {
    this.webClient = webClient;
    this.errorLogService = errorLogService;
  }

  public Future<Optional<JsonObject>> getInstancesBulkUUIDsAsync(String query, OkapiConnectionParams params) {
    Promise<Optional<JsonObject>> promise = Promise.promise();
    if (StringUtils.isEmpty(query)) {
      promise.complete(Optional.empty());
      return promise.future();
    }
    String endpoint = format(resourcesPathWithPrefix(SEARCH_IDS), params.getOkapiUrl()) + QUERY + StringUtil.urlEncode(query);
    HttpRequest<Buffer> request = webClient.getAbs(endpoint);
    request.putHeader(OKAPI_HEADER_TOKEN, params.getToken());
    request.putHeader(OKAPI_HEADER_TENANT, params.getTenantId());
    request.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    request.putHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
    if (params.getOkapiUrl().contains("https")) {
      request.ssl(true);
    }
    var parser = JsonParser.newParser().objectValueMode();
    parser
      .handler(event -> {
        var instances = event.objectValue();
        if (nonNull(instances)) {
          if (instances.getJsonArray(IDS_LIST).isEmpty()) {
            logError(new IllegalStateException(format(ERROR_MESSAGE_NO_RECORDS, endpoint)), params);
            promise.complete(Optional.empty());
          } else {
            promise.complete(Optional.of(instances));
          }
        } else {
          logError(new IllegalStateException(format(ERROR_MESSAGE_EMPTY_BODY, endpoint)), params);
          promise.complete(Optional.empty());
        }})
      .exceptionHandler(throwable -> {
        if (throwable instanceof JsonParseException) {
          logError(new IllegalStateException(format(ERROR_MESSAGE_INVALID_BODY, endpoint, throwable.getMessage())), params);
        } else {
          logError(throwable.getCause(), params);
        }
        promise.tryComplete(Optional.empty());
      });
    request.as(BodyCodec.jsonStream(parser)).send(res -> {
      if (res.failed()) {
        logError(res.cause(), params);
        promise.complete(Optional.empty());
      } else if (res.result().statusCode() != HttpStatus.SC_OK) {
        logError(new IllegalStateException(format(ERROR_MESSAGE_INVALID_STATUS_CODE, endpoint, res.result().statusCode())), params);
        promise.complete(Optional.empty());
      }
    });
    return promise.future();
  }

  private void logError(Throwable throwable, OkapiConnectionParams params) {
    LOGGER.error(throwable.getMessage(), nonNull(throwable.getCause()) ? throwable.getCause() : EMPTY);
    errorLogService.saveGeneralErrorWithMessageValues(ErrorCode.ERROR_GETTING_INSTANCES_BY_IDS.getCode(), Arrays.asList(throwable.getMessage()), StringUtils.EMPTY, params.getTenantId());
  }
}
