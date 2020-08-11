package org.folio.rest.impl;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.io.Resources;
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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.util.ExternalPathResolver.*;
import static org.junit.Assert.fail;

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
  private static final String CONFIGURATIONS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "configurations/get_configuration_response.json";
  private static final String MATERIAL_TYPES_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_material_types_response.json";
  private static final String INSTANCE_TYPES_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_instance_types_response.json";
  private static final String INSTANCE_FORMATS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_instance_formats_response.json";
  private static final String ELECTRONIC_ACCESS_RELATIONSHIPS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_electronic_access_relationships_response.json";
  private static final String ALTERNATIVE_TYPES_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_alternative_titles_response.json";
  private static final String LOAN_TYPES_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_loan_types_response.json";
  private static final String ISSUANCE_MODES_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_mode_of_issuance_response.json";
  private static final String CALLNUMBER_TYPES_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_callnumber_types_response.json";

  static Table<String, HttpMethod, List<JsonObject>> serverRqRs = HashBasedTable.create();

  private final int port;
  private final Vertx vertx;

  public MockServer(int port) {
    this.port = port;
    this.vertx = Vertx.vertx();
  }

  public void start() throws InterruptedException, ExecutionException, TimeoutException {
    // Setup Mock Server...
    logger.info("Starting mock server on port: "+port);
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

  public void close() {
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
    router.post(resourcesPath(SRS)).handler(ctx -> handleGetSRSRecord(ctx));
    router.get(resourcesPath(CONTENT_TERMS)).handler(ctx -> handleGetContentTermsRecord(ctx));
    router.get(resourcesPath(IDENTIFIER_TYPES)).handler(ctx -> handleGetIdentifierTypesRecord(ctx));
    router.get(resourcesPath(LOCATIONS)).handler(ctx -> handleGetLocationsRecord(ctx));
    router.get(resourcesPath(CONTRIBUTOR_NAME_TYPES)).handler(ctx -> handleGetContributorNameTypesRecord(ctx));
    router.get(resourcesPath(MATERIAL_TYPES)).handler(ctx -> handleGetMaterialTypesRecord(ctx));
    router.get(resourcesPath(INSTANCE_TYPES)).handler(ctx -> handleGetInstanceTypes(ctx));
    router.get(resourcesPath(INSTANCE_FORMATS)).handler(ctx -> handleGetInstanceFormats(ctx));
    router.get(resourcesPath(ELECTRONIC_ACCESS_RELATIONSHIPS)).handler(ctx -> handleGetElectronicAccessRelationships(ctx));
    router.get(resourcesPath(ALTERNATIVE_TITLE_TYPES)).handler(ctx -> handleGetAlternativeTypes(ctx));
    router.get(resourcesPath(LOAN_TYPES)).handler(ctx -> handleGetLoanTypes(ctx));
    router.get(resourcesPath(ISSUANCE_MODES)).handler(ctx -> handleGetIssuanceModes(ctx));
    router.get(resourcesPath(LOAN_TYPES)).handler(ctx -> handleGetAlternativeTypes(ctx));
    router.get(resourcesPath(USERS) + "/:id").handler(ctx -> handleGetUsersRecord(ctx));
    router.get(resourcesPath(HOLDING)).handler(ctx -> handleGetHoldingRecord(ctx));
    router.get(resourcesPath(ITEM)).handler(ctx -> handleGetItemRecord(ctx));
    router.get(resourcesPath(CONFIGURATIONS)).handler(ctx -> handleGetConfigurations(ctx));
    router.get(resourcesPath(CALLNUMBER_TYPES)).handler(ctx -> handleGetCallNumberTypes(ctx));
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
    try {
      JsonObject srsRecords = new JsonObject(RestVerticleTestBase.getMockData(SRS_RECORDS_MOCK_DATA_PATH));
      //fetch the ids from the query and remove them from the mock if not in the request
      List<String> instanceIds = ctx.getBodyAsJsonArray().getList();

      final Iterator iterator = srsRecords.getJsonArray("sourceRecords")
        .iterator();
      while (iterator.hasNext()) {
        JsonObject srsRec = (JsonObject) iterator.next();
        if (!instanceIds.contains(srsRec.getJsonObject("externalIdsHolder")
          .getString("instanceId"))) {
          iterator.remove();
        }
      }

      addServerRqRsData(HttpMethod.POST, SRS, srsRecords);
      serverResponse(ctx, 200, APPLICATION_JSON, srsRecords.encodePrettily());
    } catch (IOException e) {
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
      ctx.response()
        .setStatusCode(500)
        .end();
    }
  }

  private void handleGetMaterialTypesRecord(RoutingContext ctx) {
    logger.info("handleGet Material types Record: " + ctx.request()
      .path());
    try {
      JsonObject materialTypes = new JsonObject(RestVerticleTestBase.getMockData(MATERIAL_TYPES_RECORDS_MOCK_DATA_PATH));
      addServerRqRsData(HttpMethod.GET, MATERIAL_TYPES, materialTypes);
      serverResponse(ctx, 200, APPLICATION_JSON, materialTypes.encodePrettily());
    } catch (IOException e) {
      ctx.response()
        .setStatusCode(500)
        .end();
    }
  }

  private void handleGetInstanceTypes(RoutingContext ctx) {
    logger.info("handleGet Instance types Record: " + ctx.request()
      .path());
    try {
      JsonObject instanceTypes = new JsonObject(RestVerticleTestBase.getMockData(INSTANCE_TYPES_MOCK_DATA_PATH));
      addServerRqRsData(HttpMethod.GET, INSTANCE_TYPES, instanceTypes);
      serverResponse(ctx, 200, APPLICATION_JSON, instanceTypes.encodePrettily());
    } catch (IOException e) {
      ctx.response()
        .setStatusCode(500)
        .end();
    }
  }

  private void handleGetInstanceFormats(RoutingContext ctx) {
    logger.info("handleGet Instance formats Record: " + ctx.request()
      .path());
    try {
      JsonObject instanceFormats = new JsonObject(RestVerticleTestBase.getMockData(INSTANCE_FORMATS_MOCK_DATA_PATH));
      addServerRqRsData(HttpMethod.GET, INSTANCE_FORMATS, instanceFormats);
      serverResponse(ctx, 200, APPLICATION_JSON, instanceFormats.encodePrettily());
    } catch (IOException e) {
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
      ctx.response()
        .setStatusCode(500)
        .end();
    }
  }

  private void handleGetElectronicAccessRelationships(RoutingContext ctx) {
    logger.info("handleGet Electronic access relationship types Record: " + ctx.request()
      .path());
    try {
      JsonObject electronicAccessRelationship = new JsonObject(RestVerticleTestBase.getMockData(ELECTRONIC_ACCESS_RELATIONSHIPS_MOCK_DATA_PATH));
      addServerRqRsData(HttpMethod.GET, ELECTRONIC_ACCESS_RELATIONSHIPS, electronicAccessRelationship);
      serverResponse(ctx, 200, APPLICATION_JSON, electronicAccessRelationship.encodePrettily());
    } catch (IOException e) {
      ctx.response()
        .setStatusCode(500)
        .end();
    }
  }

  private void handleGetAlternativeTypes(RoutingContext ctx) {
    logger.info("handleGet Alternative types: " + ctx.request()
      .path());
    try {
      JsonObject alternativeTypes = new JsonObject(RestVerticleTestBase.getMockData(ALTERNATIVE_TYPES_MOCK_DATA_PATH));
      addServerRqRsData(HttpMethod.GET, ALTERNATIVE_TITLE_TYPES, alternativeTypes);
      serverResponse(ctx, 200, APPLICATION_JSON, alternativeTypes.encodePrettily());
    } catch (IOException e) {
      ctx.response()
        .setStatusCode(500)
        .end();
    }
  }

  private void handleGetLoanTypes(RoutingContext ctx) {
    logger.info("handleGet Loan types: " + ctx.request()
      .path());
    try {
      JsonObject loanTypes = new JsonObject(RestVerticleTestBase.getMockData(LOAN_TYPES_MOCK_DATA_PATH));
      addServerRqRsData(HttpMethod.GET, LOAN_TYPES, loanTypes);
      serverResponse(ctx, 200, APPLICATION_JSON, loanTypes.encodePrettily());
    } catch (IOException e) {
      ctx.response()
        .setStatusCode(500)
        .end();
    }
  }

  private void handleGetCallNumberTypes(RoutingContext ctx) {
    logger.info("handle Get call number types: ", ctx.request().path());
    try {
      JsonObject loanTypes = new JsonObject(RestVerticleTestBase.getMockData(CALLNUMBER_TYPES_MOCK_DATA_PATH));
      addServerRqRsData(HttpMethod.GET, CALLNUMBER_TYPES, loanTypes);
      serverResponse(ctx, 200, APPLICATION_JSON, loanTypes.encodePrettily());
    } catch (IOException e) {
      ctx.response().setStatusCode(500).end();
    }
  }

  private void handleGetIssuanceModes(RoutingContext ctx) {
    logger.info("handleGet issuance modes: " + ctx.request()
      .path());
    try {
      JsonObject issuanceModes = new JsonObject(RestVerticleTestBase.getMockData(ISSUANCE_MODES_MOCK_DATA_PATH));
      addServerRqRsData(HttpMethod.GET, ISSUANCE_MODES, issuanceModes);
      serverResponse(ctx, 200, APPLICATION_JSON, issuanceModes.encodePrettily());
    } catch (IOException e) {
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

  private void handleGetConfigurations(RoutingContext ctx) {
    logger.info("handleGetRulesFromModConfigurations got: " + ctx.request()
      .path());
    try {
      JsonObject rulesFromConfig = new JsonObject(RestVerticleTestBase.getMockData(CONFIGURATIONS_MOCK_DATA_PATH));
      URL url = Resources.getResource("rules/rulesDefault.json");
      String rules = Resources.toString(url, StandardCharsets.UTF_8);
      rulesFromConfig.getJsonArray("configs")
        .stream()
        .map(object -> (JsonObject) object)
        .forEach(obj -> obj.put("value", rules));

      addServerRqRsData(HttpMethod.GET, CONFIGURATIONS, rulesFromConfig);
      serverResponse(ctx, 200, APPLICATION_JSON, rulesFromConfig.encodePrettily());
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
