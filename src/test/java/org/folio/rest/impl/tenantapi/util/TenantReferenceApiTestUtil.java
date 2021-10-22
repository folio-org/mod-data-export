package org.folio.rest.impl.tenantapi.util;

import io.restassured.http.Header;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.jaxrs.model.TenantJob;
import org.folio.rest.tools.utils.ModuleName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.folio.rest.impl.StorageTestSuite.URL_TO_HEADER;
import static org.junit.jupiter.api.Assertions.fail;

public class TenantReferenceApiTestUtil {

  public static final String LOAD_SYNC_PARAMETER = "loadSync";
  private static final int TENANT_OP_WAITINGTIME = 60000;

  private TenantReferenceApiTestUtil() {
  }

  public static TenantAttributes prepareTenantBody(Boolean isLoadSampleData, Boolean isLoadReferenceData) {
    TenantAttributes tenantAttributes = new TenantAttributes();

    String moduleId = String.format("%s", ModuleName.getModuleName());
    List<Parameter> parameters = new ArrayList<>();
    parameters.add(new Parameter().withKey("loadReference").withValue(isLoadReferenceData.toString()));
    parameters.add(new Parameter().withKey("loadSample").withValue(isLoadSampleData.toString()));
    parameters.add(new Parameter().withKey(LOAD_SYNC_PARAMETER).withValue("true"));

    tenantAttributes.withModuleTo(moduleId)
      .withParameters(parameters);

    return tenantAttributes;
  }

  public static TenantJob postTenant(Header tenantHeader, TenantAttributes tenantAttributes) {
    CompletableFuture<TenantJob> future = new CompletableFuture<>();
    TenantClient tClient =  new TenantClient(URL_TO_HEADER.getValue(), tenantHeader.getValue(), null);
    try {
      tClient.postTenant(tenantAttributes, event -> {
        if (event.failed()) {
          future.completeExceptionally(event.cause());
        } else {
          TenantJob tenantJob = event.result().bodyAsJson(TenantJob.class);
          tClient.getTenantByOperationId(tenantJob.getId(), TENANT_OP_WAITINGTIME, result -> {
            if(result.failed()) {
              future.completeExceptionally(result.cause());
            } else {
              future.complete(tenantJob);
            }
          });
        }
      });
      return future.get(60, TimeUnit.SECONDS);
    } catch (Exception e) {
      fail(e);
      return null;
    }
  }

  public static void deleteTenant(TenantJob tenantJob, Header tenantHeader) {
    TenantClient tenantClient = new TenantClient(URL_TO_HEADER.getValue(), tenantHeader.getValue(), null);

    if (tenantJob != null) {
      CompletableFuture<Void> completableFuture = new CompletableFuture<>();
      tenantClient.deleteTenantByOperationId(tenantJob.getId(), event -> {
        if (event.failed()) {
          completableFuture.completeExceptionally(event.cause());
        } else {
          completableFuture.complete(null);
        }
      });
      try {
        completableFuture.get(60, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        fail(e);
      }

    }

  }

  public static void purge(Header tenantHeader) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    TenantClient tClient =  new TenantClient(URL_TO_HEADER.getValue(), tenantHeader.getValue(), null);
    TenantAttributes tenantAttributes = prepareTenantBody(false, false).withPurge(true);
    try {
      tClient.postTenant(tenantAttributes, event -> {
        if (event.failed()) {
          future.completeExceptionally(event.cause());
        } else {
          future.complete(null);
        }
      });
      future.get(60, TimeUnit.SECONDS);
    } catch (Exception e) {
      fail(e);
    }
  }
}
