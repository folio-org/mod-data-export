package org.folio.clients;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.Response;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.util.OkapiConnectionParams;

import java.util.function.Function;

public final class ClientUtilAsync {

  private ClientUtilAsync() {
  }

  public static Future<JsonObject> getRequest(String endpoint, OkapiConnectionParams params) {
    Promise<JsonObject> promise = Promise.promise();
    final HttpClientInterface httpClient = getHttpClient(params);
    try {
      httpClient.request(endpoint, params.getHeaders())
        .whenComplete(((response, throwable) -> processResponse(promise, response, throwable, Response::getBody)));
    } catch (Exception e) {
      promise.fail(e);
    }
    return promise.future();
  }

  static HttpClientInterface getHttpClient(OkapiConnectionParams params) {
    final String okapiURL = params.getOkapiUrl();
    final String tenantId = TenantTool.calculateTenantId(params.getTenantId());

    return HttpClientFactory.getHttpClient(okapiURL, tenantId);
  }

  private static <T> void processResponse(Promise<T> promise, Response response, Throwable ex,
                                          Function<Response, T> completeFunction) {
    if (ex != null) {
      promise.fail(ex);
    } else if (response.getException() != null) {
      promise.fail(response.getException());
    } else if (isFailedResponseCode(response)) {
      String failureMessage =
        response.getError() != null ? response.getError().encode() : "Error code: " + response.getCode();
      promise.fail(failureMessage);
    } else {
      promise.complete(completeFunction.apply(response));
    }
  }

  private static boolean isFailedResponseCode(Response response) {
    return !Response.isSuccess(response.getCode());
  }
}
