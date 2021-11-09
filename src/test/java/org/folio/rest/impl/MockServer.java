package org.folio.rest.impl;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.io.Resources;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.util.ExternalPathResolver.*;
import static org.junit.Assert.fail;

public class MockServer {
  private static final Logger logger = LogManager.getLogger(MockServer.class);

  // Mock data paths
  public static final String BASE_MOCK_DATA_PATH = "mockData/";
  private static final String INSTANCE_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_instance_response_in000005.json";
  private static final String HOLDING_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/holdings_in000005.json";
  private static final String ITEM_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/items_in000005.json";
  private static final String HOLDING_RECORDS_IN00041_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/holdings_in00041.json";
  private static final String ITEM_RECORDS_IN00041_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/items_in00041.json";
  private static final String SRS_MARC_BIB_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "srs/get_marc_bib_records_response.json";
  private static final String SRS_MARC_HOLDING_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "srs/get_marc_holdings_records_response.json";
  private static final String USERS_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "user/get_user_response.json";
  private static final String CONTENT_TERMS_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_nature_of_content_terms_response.json";
  private static final String IDENTIFIER_TYPES_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_identifier_types_response.json";
  private static final String CONTRIBUTOR_NAME_TYPES_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_contributor_name_types_response.json";
  private static final String LOCATIONS_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_locations_response.json";
  private static final String LIBRARIES_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_libraries_response.json";
  private static final String CAMPUSES_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_campuses_response.json";
  private static final String INSTITUTIONS_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_institutions_response.json";
  private static final String CONFIGURATIONS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "configurations/get_configuration_response.json";
  private static final String CONFIGURATIONS_MOCK_DATA_PATH_FOR_HOST = BASE_MOCK_DATA_PATH + "configurations/get_host_configuration_response.json";
  private static final String MATERIAL_TYPES_RECORDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_material_types_response.json";
  private static final String INSTANCE_TYPES_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_instance_types_response.json";
  private static final String INSTANCE_FORMATS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_instance_formats_response.json";
  private static final String ELECTRONIC_ACCESS_RELATIONSHIPS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_electronic_access_relationships_response.json";
  private static final String ALTERNATIVE_TYPES_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_alternative_titles_response.json";
  private static final String LOAN_TYPES_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_loan_types_response.json";
  private static final String ISSUANCE_MODES_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_mode_of_issuance_response.json";
  private static final String CALL_NUMBER_TYPES_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_call_number_types_response.json";
  private static final String HOLDING_NOTE_TYPES_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_holding_note_types_response.json";
  private static final String ITEM_NOTE_TYPES_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_item_note_types_response.json";
  private static final String INSTANCE_BULK_IDS_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_instance_bulk_ids_response.json";
  private static final String INSTANCE_BULK_IDS_ALL_VALID_MOCK_DATA_PATH = BASE_MOCK_DATA_PATH + "inventory/get_valid_instance_bulk_ids_response.json";
  private static final String INSTANCE_BULK_IDS_WITH_RANDOM = BASE_MOCK_DATA_PATH + "inventory/get_instance_bulk_ids_with_random.json";

  static Table<String, HttpMethod, List<JsonObject>> serverRqRs = HashBasedTable.create();

  private final int port;
  private final Vertx vertx;

  public MockServer(int port) {
    this.port = port;
    this.vertx = Vertx.vertx();
  }

  public void start() throws InterruptedException, ExecutionException, TimeoutException {
    // Setup Mock Server...
    logger.info("Starting mock server on port: " + port);
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
    router.get(resourcesPath(LIBRARIES)).handler(ctx -> handleGetLibrariesRecord(ctx));
    router.get(resourcesPath(CAMPUSES)).handler(ctx -> handleGetCampusesRecord(ctx));
    router.get(resourcesPath(INSTITUTIONS)).handler(ctx -> handleGetInstitutionsRecord(ctx));
    router.get(resourcesPath(CONTRIBUTOR_NAME_TYPES)).handler(ctx -> handleGetContributorNameTypesRecord(ctx));
    router.get(resourcesPath(MATERIAL_TYPES)).handler(ctx -> handleGetMaterialTypesRecord(ctx));
    router.get(resourcesPath(INSTANCE_TYPES)).handler(ctx -> handleGetInstanceTypes(ctx));
    router.get(resourcesPath(INSTANCE_FORMATS)).handler(ctx -> handleGetInstanceFormats(ctx));
    router.get(resourcesPath(ELECTRONIC_ACCESS_RELATIONSHIPS)).handler(ctx -> handleGetElectronicAccessRelationships(ctx));
    router.get(resourcesPath(ALTERNATIVE_TITLE_TYPES)).handler(ctx -> handleGetAlternativeTypes(ctx));
    router.get(resourcesPath(LOAN_TYPES)).handler(ctx -> handleGetLoanTypes(ctx));
    router.get(resourcesPath(ISSUANCE_MODES)).handler(ctx -> handleGetIssuanceModes(ctx));
    router.get(resourcesPath(LOAN_TYPES)).handler(ctx -> handleGetAlternativeTypes(ctx));
    router.get(resourcesPath(CALL_NUMBER_TYPES)).handler(ctx -> handleGetCallNumberTypes(ctx));
    router.get(resourcesPath(HOLDING_NOTE_TYPES)).handler(ctx -> handleGetHoldingNoteTypes(ctx));
    router.get(resourcesPath(ITEM_NOTE_TYPES)).handler(ctx -> handleGetItemNoteTypes(ctx));
    router.get(resourcesPath(USERS) + "/:id").handler(ctx -> handleGetUsersRecord(ctx));
    router.get(resourcesPath(HOLDING)).handler(ctx -> handleGetHoldingRecord(ctx));
    router.get(resourcesPath(ITEM)).handler(ctx -> handleGetItemRecord(ctx));
    router.get(resourcesPath(CONFIGURATIONS)).handler(ctx -> handleGetConfigurations(ctx));
    router.get(resourcesPath(SEARCH_IDS)).handler(ctx -> handleGetInstanceBulkIds(ctx));
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
        getMockResponseFromPathWith200Status(ITEM_RECORDS_IN00041_MOCK_DATA_PATH, ITEM, ctx);
      } else {
        getMockResponseFromPathWith200Status(ITEM_RECORDS_MOCK_DATA_PATH, ITEM, ctx);
      }
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
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
        getMockResponseFromPathWith200Status(HOLDING_RECORDS_IN00041_MOCK_DATA_PATH, HOLDING, ctx);
      } else {
        getMockResponseFromPathWith200Status(HOLDING_RECORDS_MOCK_DATA_PATH, HOLDING, ctx);
      }
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetInstanceRecord(RoutingContext ctx) {
    logger.info("handleGetInstanceRecord got: " + ctx.request()
      .path());
    String query = ctx.request().getParam("query");
    if (StringUtils.isNotEmpty(query) && query.contains("7c29e100-095f-11eb-adc1-0242ac120002")) {
      serverResponse(ctx, 500, APPLICATION_JSON, null);
    }
    JsonObject instance = null;
    try {
      getMockResponseFromPathWith200Status(INSTANCE_RECORDS_MOCK_DATA_PATH, INSTANCE, ctx);
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetSRSRecord(RoutingContext ctx) {
    logger.info("handleGetSRSRecord got: " + ctx.request()
      .path());
    try {
      //fetch the ids from the query and remove them from the mock if not in the request
      String idType = ctx.request().getParam("idType");
      List<String> ids = ctx.getBodyAsJsonArray().getList();
      String path = idType.equalsIgnoreCase("instance") ? SRS_MARC_BIB_RECORDS_MOCK_DATA_PATH : SRS_MARC_HOLDING_RECORDS_MOCK_DATA_PATH;
      String fieldKey = idType.equalsIgnoreCase("instance") ? "instanceId" : "holdingId";
      JsonObject srsRecords = new JsonObject(RestVerticleTestBase.getMockData(path));

      final Iterator iterator = srsRecords.getJsonArray("sourceRecords")
        .iterator();
      while (iterator.hasNext()) {
        JsonObject srsRec = (JsonObject) iterator.next();
        if (!ids.contains(srsRec.getJsonObject("externalIdsHolder")
          .getString(fieldKey))) {
          iterator.remove();
        }
      }

      addServerRqRsData(HttpMethod.POST, SRS, srsRecords);
      serverResponse(ctx, 200, APPLICATION_JSON, srsRecords.encodePrettily());
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetContentTermsRecord(RoutingContext ctx) {
    logger.info("handleGet Nature of content terms Record got: " + ctx.request()
      .path());
    try {
      getMockResponseFromPathWith200Status(CONTENT_TERMS_RECORDS_MOCK_DATA_PATH, CONTENT_TERMS, ctx);
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetIdentifierTypesRecord(RoutingContext ctx) {
    logger.info("handleGet Identifier types Record got: " + ctx.request()
      .path());
    try {
      getMockResponseFromPathWith200Status(IDENTIFIER_TYPES_RECORDS_MOCK_DATA_PATH, IDENTIFIER_TYPES, ctx);
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetLibrariesRecord(RoutingContext ctx) {
    logger.info("handleGet Libraries Record: " + ctx.request()
      .path());
    try {
      getMockResponseFromPathWith200Status(LIBRARIES_RECORDS_MOCK_DATA_PATH, LIBRARIES, ctx);
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetCampusesRecord(RoutingContext ctx) {
    logger.info("handleGet Campuses Record: " + ctx.request()
      .path());
    try {
      getMockResponseFromPathWith200Status(CAMPUSES_RECORDS_MOCK_DATA_PATH, CAMPUSES, ctx);
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetInstitutionsRecord(RoutingContext ctx) {
    logger.info("handleGet Institutions Record: " + ctx.request()
      .path());
    try {
      getMockResponseFromPathWith200Status(INSTITUTIONS_RECORDS_MOCK_DATA_PATH, INSTITUTIONS, ctx);
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetLocationsRecord(RoutingContext ctx) {
    logger.info("handleGet Locations Record: " + ctx.request()
      .path());
    try {
      getMockResponseFromPathWith200Status(LOCATIONS_RECORDS_MOCK_DATA_PATH, LOCATIONS, ctx);
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetMaterialTypesRecord(RoutingContext ctx) {
    logger.info("handleGet Material types Record: " + ctx.request()
      .path());
    try {
      getMockResponseFromPathWith200Status(MATERIAL_TYPES_RECORDS_MOCK_DATA_PATH, MATERIAL_TYPES, ctx);
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetInstanceTypes(RoutingContext ctx) {
    logger.info("handleGet Instance types Record: " + ctx.request()
      .path());
    try {
      getMockResponseFromPathWith200Status(INSTANCE_TYPES_MOCK_DATA_PATH, INSTANCE_TYPES, ctx);
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetInstanceFormats(RoutingContext ctx) {
    logger.info("handleGet Instance formats Record: " + ctx.request()
      .path());
    try {
      getMockResponseFromPathWith200Status(INSTANCE_FORMATS_MOCK_DATA_PATH, INSTANCE_FORMATS, ctx);
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetContributorNameTypesRecord(RoutingContext ctx) {
    logger.info("handleGet ContributorName types Record got: " + ctx.request()
      .path());
    try {
      getMockResponseFromPathWith200Status(CONTRIBUTOR_NAME_TYPES_RECORDS_MOCK_DATA_PATH, CONTRIBUTOR_NAME_TYPES, ctx);
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetElectronicAccessRelationships(RoutingContext ctx) {
    logger.info("handleGet Electronic access relationship types Record: " + ctx.request()
      .path());
    try {
      getMockResponseFromPathWith200Status(ELECTRONIC_ACCESS_RELATIONSHIPS_MOCK_DATA_PATH, ELECTRONIC_ACCESS_RELATIONSHIPS, ctx);
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetAlternativeTypes(RoutingContext ctx) {
    logger.info("handleGet Alternative types: " + ctx.request()
      .path());
    try {
      getMockResponseFromPathWith200Status(ALTERNATIVE_TYPES_MOCK_DATA_PATH, ALTERNATIVE_TITLE_TYPES, ctx);
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetLoanTypes(RoutingContext ctx) {
    logger.info("handleGet Loan types: " + ctx.request()
      .path());
    try {
      getMockResponseFromPathWith200Status(LOAN_TYPES_MOCK_DATA_PATH, LOAN_TYPES, ctx);
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetIssuanceModes(RoutingContext ctx) {
    logger.info("handleGet issuance modes: " + ctx.request()
      .path());
    try {
      getMockResponseFromPathWith200Status(ISSUANCE_MODES_MOCK_DATA_PATH, ISSUANCE_MODES, ctx);
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetCallNumberTypes(RoutingContext ctx) {
    logger.info("handleGet call number types: " + ctx.request()
      .path());
    try {
      getMockResponseFromPathWith200Status(CALL_NUMBER_TYPES_MOCK_DATA_PATH, CALL_NUMBER_TYPES, ctx);
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetHoldingNoteTypes(RoutingContext ctx) {
    logger.info("handle Get holding note types: " + ctx.request().path());
    try {
      getMockResponseFromPathWith200Status(HOLDING_NOTE_TYPES_MOCK_DATA_PATH, HOLDING_NOTE_TYPES, ctx);
    } catch (IOException e) {
      ctx.response().setStatusCode(500).end();
    }
  }

  private void handleGetItemNoteTypes(RoutingContext ctx) {
    logger.info("handle Get item note types: " + ctx.request().path());
    try {
      getMockResponseFromPathWith200Status(ITEM_NOTE_TYPES_MOCK_DATA_PATH, ITEM_NOTE_TYPES, ctx);
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetUsersRecord(RoutingContext ctx) {
    logger.info("handleGetUsersRecord got: " + ctx.request()
      .path());
    try {
      getMockResponseFromPathWith200Status(USERS_RECORDS_MOCK_DATA_PATH, USERS, ctx);
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetInstanceBulkIds(RoutingContext ctx) {
    logger.info("Handle get instance bulk ids: " + ctx.request().path());
    try {
      JsonObject bulkIds;
      if (ctx.request().getParam("query").contains("(languages=\"eng\")")) {
        getMockResponseFromPathWith200Status(INSTANCE_BULK_IDS_ALL_VALID_MOCK_DATA_PATH, SEARCH_IDS, ctx);
      } else if (ctx.request().getParam("query").contains("(languages=\"uk\")")) {
        getMockResponseFromPathWith200Status(INSTANCE_BULK_IDS_WITH_RANDOM, SEARCH_IDS, ctx);
      } else if (ctx.request().getParam("query").contains("inventory 500")) {
        mockResponseWith500Status(ctx);
      } else if (ctx.request().getParam("query").contains("empty json response")) {
        ctx.response().setStatusCode(200).putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON).end();
      } else if (ctx.request().getParam("query").contains("invalid json returned")) {
        ctx.response().setStatusCode(200).putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON).end("{qwe");
      } else if (ctx.request().getParam("query").contains("bad request")) {
        ctx.response().setStatusCode(400).putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON).end();
      } else {
        getMockResponseFromPathWith200Status(INSTANCE_BULK_IDS_MOCK_DATA_PATH, SEARCH_IDS, ctx);
      }
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
    }
  }

  private void handleGetConfigurations(RoutingContext ctx) {
    logger.info("handleGetRulesFromModConfigurations got: " + ctx.request()
      .path());
    try {
      if (ctx.request().getParam("query").contains("FOLIO_HOST")) {
        getMockResponseFromPathWith200Status(CONFIGURATIONS_MOCK_DATA_PATH_FOR_HOST, CONFIGURATIONS, ctx);
      } else if (ctx.request().getParam("query").contains("FAIL")) {
        ctx.response()
          .setStatusCode(500)
          .end();
      } else {
        JsonObject rulesFromConfig = new JsonObject(RestVerticleTestBase.getMockData(CONFIGURATIONS_MOCK_DATA_PATH));
        URL url = Resources.getResource("rules/rulesDefault.json");
        String rules = Resources.toString(url, StandardCharsets.UTF_8);
        rulesFromConfig.getJsonArray("configs")
          .stream()
          .map(object -> (JsonObject) object)
          .forEach(obj -> obj.put("value", rules));

        addServerRqRsData(HttpMethod.GET, CONFIGURATIONS, rulesFromConfig);
        serverResponse(ctx, 200, APPLICATION_JSON, rulesFromConfig.encodePrettily());
      }
    } catch (IOException e) {
      mockResponseWith500Status(ctx);
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

  private JsonObject getMockResponseFromPathWith200Status(String mockDataPath, String uri, RoutingContext ctx) throws IOException {
    JsonObject jsonObject = new JsonObject(RestVerticleTestBase.getMockData(mockDataPath));
    addServerRqRsData(HttpMethod.GET, uri, jsonObject);
    serverResponse(ctx, 200, APPLICATION_JSON, jsonObject.encodePrettily());
    return jsonObject;
  }

  private void mockResponseWith500Status(RoutingContext ctx) {
    ctx.response().setStatusCode(500).end();
  }

}
