package org.folio.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.util.ExternalPathResolver.CONTENT_TERMS;
import static org.folio.util.ExternalPathResolver.IDENTIFIER_TYPES;
import static org.folio.util.ExternalPathResolver.CONTRIBUTOR_NAME_TYPES;
import static org.folio.util.ExternalPathResolver.INSTANCE;
import static org.folio.util.ExternalPathResolver.LOCATIONS;
import static org.folio.util.ExternalPathResolver.SRS;
import static org.folio.util.ExternalPathResolver.USERS;
import static org.folio.util.ExternalPathResolver.HOLDING;
import static org.folio.util.ExternalPathResolver.ITEM;
import static org.folio.util.ExternalPathResolver.resourcesPath;
import static org.junit.Assert.fail;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MockServer {
  private static final Logger logger = LoggerFactory.getLogger(MockServer.class);

  // Mock data paths
  public static final String BASE_MOCK_DATA_PATH = "mockData/";
  private static final String INSTANCE_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_instance_response_in000005.json";
  private static final String HOLDING_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/holdings_in000005.json";
  private static final String ITEM_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/items_in000005.json";
  private static final String HOLDING_RECORDS_IN00041_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/holdings_in00041.json";
  private static final String ITEM_RECORDS_IN00041_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/items_in00041.json";
  private static final String SRS_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "srs/get_records_response.json";
  private static final String USERS_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "user/get_user_response.json";
  private static final String CONTENT_TERMS_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_nature_of_content_terms_response.json";
  private static final String IDENTIFIER_TYPES_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_identifier_types_response.json";
  private static final String CONTRIBUTOR_NAME_TYPES_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_contributor_name_types_response.json";
  private static final String LOCATIONS_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_locations_response.json";

  static Table<String, HttpMethod, List<JsonObject>> serverRqRs = HashBasedTable.create();

  private final int port;
  private final Vertx vertx;

  MockServer(int port) {
    this.port = port;
    this.vertx = Vertx.vertx();
  }

  void start() throws InterruptedException, ExecutionException, TimeoutException {
    // Setup Mock Server...
    HttpServer server = vertx.createHttpServer();
    CompletableFuture<HttpServer> deploymentComplete = new CompletableFuture<>();
    server.requestHandler(defineRoutes())
      .listen(port, result -> {
        if (result.succeeded()) {
          deploymentComplete.complete(result.result());
        } else {
          deploymentComplete.completeExceptionally(result.cause());
        }
      });
    deploymentComplete.get(60, TimeUnit.SECONDS);
  }

  void close() {
    vertx.close(res -> {
      if (res.failed()) {
        logger.error("Failed to shut down mock server", res.cause());
        fail(res.cause()
          .getMessage());
      } else {
        logger.info("Successfully shut down mock server");
      }
    });
  }

  public static void release() {
    serverRqRs.clear();
  }

  private Router defineRoutes() {
    Router router = Router.router(vertx);

    router.route()
      .handler(BodyHandler.create());

    router.get(resourcesPath(INSTANCE)).handler(ctx -> handleGetInstanceRecord(ctx));
    router.get(resourcesPath(SRS)).handler(ctx -> handleGetSRSRecord(ctx));
    router.get(resourcesPath(CONTENT_TERMS)).handler(ctx -> handleGetContentTermsRecord(ctx));
    router.get(resourcesPath(IDENTIFIER_TYPES)).handler(ctx -> handleGetIdentifierTypesRecord(ctx));
    router.get(resourcesPath(LOCATIONS)).handler(ctx -> handleGetLocationsRecord(ctx));
    router.get(resourcesPath(CONTRIBUTOR_NAME_TYPES)).handler(ctx -> handleGetContributorNameTypesRecord(ctx));
    router.get(resourcesPath(USERS) + "/:id").handler(ctx -> handleGetUsersRecord(ctx));
    router.get(resourcesPath(HOLDING)).handler(ctx -> handleGetHoldingRecord(ctx));
    router.get(resourcesPath(ITEM)).handler(ctx -> handleGetItemRecord(ctx));

    return router;
  }


  private void handleGetItemRecord(RoutingContext ctx) {
    logger.info("handleGetInstanceRecord got: " + ctx.request()
      .path());
    try {
      JsonObject item;
      if (ctx.request()
        .getParam("query")
        .contains("ae573875-fbc8-40e7-bda7-0ac283354226")) {
        item = new JsonObject(RestVerticleTestBase.getMockData(ITEM_RECORDS_IN00041_MOCK_DATA_PATH));
      } else {
        item = new JsonObject(RestVerticleTestBase.getMockData(ITEM_RECORDS_MOCK_DATA_PATH));
      }
      addServerRqRsData(HttpMethod.GET, ITEM, item);
      serverResponse(ctx, 200, APPLICATION_JSON, item.encodePrettily());
    } catch (IOException e) {
      ctx.response()
        .setStatusCode(500)
        .end();
    }
  }

  private void handleGetHoldingRecord(RoutingContext ctx) {
    logger.info("handleGetInstanceRecord got: " + ctx.request()
      .path());
    try {
      JsonObject holding;
      if (ctx.request()
        .getParam("query")
        .contains("ae573875-fbc8-40e7-bda7-0ac283354226")) {
        holding = new JsonObject(RestVerticleTestBase.getMockData(HOLDING_RECORDS_IN00041_MOCK_DATA_PATH));
      } else {
        holding = new JsonObject(RestVerticleTestBase.getMockData(HOLDING_RECORDS_MOCK_DATA_PATH));
      }
      addServerRqRsData(HttpMethod.GET, HOLDING, holding);
      serverResponse(ctx, 200, APPLICATION_JSON, holding.encodePrettily());
    } catch (IOException e) {
      ctx.response()
        .setStatusCode(500)
        .end();
    }
  }

  private void handleGetInstanceRecord(RoutingContext ctx) {
    logger.info("handleGetInstanceRecord got: " + ctx.request()
      .path());
    JsonObject instance = null;
    try {
      instance = new JsonObject(RestVerticleTestBase.getMockData(INSTANCE_RECORDS_MOCK_DATA_PATH));
      addServerRqRsData(HttpMethod.GET, INSTANCE, instance);
      serverResponse(ctx, 200, APPLICATION_JSON, instance.encodePrettily());
    } catch (IOException e) {
      ctx.response()
        .setStatusCode(500)
        .end();
    }
  }

  private void handleGetSRSRecord(RoutingContext ctx) {
    logger.info("handleGetSRSRecord got: " + ctx.request()
      .path());
    String query = ctx.request()
      .query();
    try {
      JsonObject srsRecords;
      if (query.contains("7fbd5d84-62d1-44c6-9c45-6cb173998bbd")) {
        srsRecords = buildEmptyCollection("records");
        } else if (query.contains("b84653c8-1baf-488b-8616-0f4dbaf8119e")) {
          JsonArray ar = new JsonArray(RestVerticleTestBase.getMockData(SRS_RECORDS_MOCK_DATA_PATH));
          srsRecords = ar.getJsonObject(0);
      } else {
        srsRecords = new JsonObject(RestVerticleTestBase.getMockData(SRS_RECORDS_MOCK_DATA_PATH));
      }
      addServerRqRsData(HttpMethod.GET, SRS, srsRecords);
      serverResponse(ctx, 200, APPLICATION_JSON, srsRecords.encodePrettily());
    } catch (IOException e) {
      System.err.println(e);
      ctx.response()
        .setStatusCode(500)
        .end();
    }
  }


  private void handleGetContentTermsRecord(RoutingContext ctx) {
    logger.info("handleGet Nature of content terms Record got: " + ctx.request()
      .path());
    try {
      JsonObject contentTerms = new JsonObject(RestVerticleTestBase.getMockData(CONTENT_TERMS_RECORDS_MOCK_DATA_PATH));
      addServerRqRsData(HttpMethod.GET, CONTENT_TERMS, contentTerms);
      serverResponse(ctx, 200, APPLICATION_JSON, contentTerms.encodePrettily());
    } catch (IOException e) {
      System.err.println(e);
      ctx.response()
        .setStatusCode(500)
        .end();
    }
  }

  private void handleGetIdentifierTypesRecord(RoutingContext ctx) {
    logger.info("handleGet Identifier types Record got: " + ctx.request()
      .path());
    try {
      JsonObject identifierTypes = new JsonObject(RestVerticleTestBase.getMockData(IDENTIFIER_TYPES_RECORDS_MOCK_DATA_PATH));
      addServerRqRsData(HttpMethod.GET, IDENTIFIER_TYPES, identifierTypes);
      serverResponse(ctx, 200, APPLICATION_JSON, identifierTypes.encodePrettily());
    } catch (IOException e) {
      System.err.println(e);
      ctx.response()
        .setStatusCode(500)
        .end();
    }
  }

  private void handleGetLocationsRecord(RoutingContext ctx) {
    logger.info("handleGet Locations Record: " + ctx.request()
      .path());
    try {
      JsonObject locations = new JsonObject(RestVerticleTestBase.getMockData(LOCATIONS_RECORDS_MOCK_DATA_PATH));
      addServerRqRsData(HttpMethod.GET, LOCATIONS, locations);
      serverResponse(ctx, 200, APPLICATION_JSON, locations.encodePrettily());
    } catch (IOException e) {
      System.err.println(e);
      ctx.response()
        .setStatusCode(500)
        .end();
    }
  }


  private void handleGetContributorNameTypesRecord(RoutingContext ctx) {
    logger.info("handleGet ContributorName types Record got: " + ctx.request()
    .path());
  try {
    JsonObject contributorTypes = new JsonObject(RestVerticleTestBase.getMockData(CONTRIBUTOR_NAME_TYPES_RECORDS_MOCK_DATA_PATH));
    addServerRqRsData(HttpMethod.GET, CONTRIBUTOR_NAME_TYPES, contributorTypes);
    serverResponse(ctx, 200, APPLICATION_JSON, contributorTypes.encodePrettily());
  } catch (IOException e) {
    System.err.println(e);
    ctx.response()
      .setStatusCode(500)
      .end();
  }
}


  private void handleGetUsersRecord(RoutingContext ctx) {
    logger.info("handleGetUsersRecord got: " + ctx.request()
      .path());
    try {
      JsonObject user = new JsonObject(RestVerticleTestBase.getMockData(USERS_RECORDS_MOCK_DATA_PATH));
      addServerRqRsData(HttpMethod.GET, USERS, user);
      serverResponse(ctx, 200, APPLICATION_JSON, user.encodePrettily());
    } catch (IOException e) {
      ctx.response()
        .setStatusCode(500)
        .end();
    }
  }


  private void serverResponse(RoutingContext ctx, int statusCode, String contentType, String body) {
    ctx.response()
      .setStatusCode(statusCode)
      .putHeader(HttpHeaders.CONTENT_TYPE, contentType)
      .end(body);
  }


  private static void addServerRqRsData(HttpMethod method, String objName, JsonObject data) {
    List<JsonObject> entries = serverRqRs.get(objName, method);
    if (entries == null) {
      entries = new ArrayList<>();
    }
    entries.add(data);
    serverRqRs.put(objName, method, entries);
  }

  public static List<JsonObject> getServerRqRsData(HttpMethod method, String objName) {
    return serverRqRs.get(objName, method);
  }

  private JsonObject buildEmptyCollection(String entryType) {
    JsonObject result = new JsonObject();
    result.put(entryType, new JsonArray());
    result.put("totalRecords", 0);
    return result;
  }
}
