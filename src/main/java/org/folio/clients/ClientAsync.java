package org.folio.clients;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.folio.util.OkapiConnectionParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MediaType;
import java.lang.invoke.MethodHandles;

import static org.folio.rest.RestVerticle.MODULE_SPECIFIC_ARGS;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;

@Component
public class ClientAsync {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int LOOKUP_TIMEOUT = Integer
    .parseInt(MODULE_SPECIFIC_ARGS.getOrDefault("lookup.timeout", "1000"));

  private final WebClient webClient;

  public ClientAsync(@Autowired Vertx vertx) {
    this.webClient = WebClient.create(vertx,
      new WebClientOptions().setConnectTimeout(LOOKUP_TIMEOUT).setIdleTimeout(LOOKUP_TIMEOUT));
  }

  public Future<JsonObject> getRequest(String endpoint, OkapiConnectionParams params) {
    Promise<JsonObject> promise = Promise.promise();
    HttpRequest<Buffer> request = buildRequest(webClient, endpoint, params);
    LOGGER.info("Calling GET {}", endpoint);
    request
      .send(ar -> {
        if (ar.failed()) {
          LOGGER.error("Exception while calling {}", endpoint, ar.cause());
          promise.fail(ar.cause());
          return;
        }
        HttpResponse<Buffer> response = ar.result();
        if (response.statusCode() != org.folio.HttpStatus.HTTP_OK.toInt()) {
          LOGGER.error("Exception while calling {}", endpoint, ar.cause());
          promise.fail(ar.cause());
        } else {
          promise.complete(response.bodyAsJsonObject());
        }
      });
    return promise.future();
  }

  private HttpRequest<Buffer> buildRequest(WebClient webClient, String endpoint, OkapiConnectionParams params) {
    HttpRequest<Buffer> request = webClient.getAbs(endpoint);
    return request
      .putHeader(OKAPI_HEADER_TOKEN, params.getToken())
      .putHeader(OKAPI_HEADER_TENANT, params.getTenantId())
      .putHeader(io.vertx.core.http.HttpHeaders.CONTENT_TYPE.toString(), MediaType.APPLICATION_JSON)
      .putHeader(HttpHeaders.ACCEPT.toString(), MediaType.APPLICATION_JSON);
  }
}
