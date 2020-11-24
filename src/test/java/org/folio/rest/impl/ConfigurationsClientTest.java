package org.folio.rest.impl;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.commons.collections4.map.HashedMap;
import org.folio.clients.ConfigurationsClient;
import org.folio.processor.rule.Rule;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.service.ApplicationTestConfig;
import org.folio.service.logs.ErrorLogService;
import org.folio.spring.SpringContextUtil;
import org.folio.util.HelperUtils;
import org.folio.util.OkapiConnectionParams;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
class ConfigurationsClientTest extends RestVerticleTestBase {
  private static final String DEFAULT_LEADER_FIELD_NAME = "leader";
  private static final String DEFAULT_LEADER_FIELD_DESCRIPTION = "Leader";
  private static final String DEFAULT_LEADER_TRANSLATION_FUNCTION = "set_17-19_positions";
  private static final String DEFAULT_LEADER_TRANSLATION_PARAMETERS_POS17 = "3";
  private static final String DEFAULT_LEADER_TRANSLATION_PARAMETERS_POS18 = "c";
  private static final String DEFAULT_LEADER_TRANSLATION_PARAMETERS_POS19 = " ";
  private static final String JOB_EXECUTION_ID = UUID.randomUUID().toString();
  private static OkapiConnectionParams okapiConnectionParams;

  @Autowired
  ConfigurationsClient configurationsClient;
  @Autowired
  ErrorLogService errorLogService;


  public ConfigurationsClientTest() {
    Context vertxContext = Vertx.vertx().getOrCreateContext();
    SpringContextUtil.init(vertxContext.owner(), vertxContext, ApplicationTestConfig.class);
    SpringContextUtil.autowireDependencies(this, vertxContext);
  }

  @BeforeAll
  public static void beforeClass() {
    Map<String, String> headers = new HashedMap<>();
    headers.put(OKAPI_HEADER_TENANT, TENANT_ID);
    headers.put(OKAPI_HEADER_URL, MOCK_OKAPI_URL);
    okapiConnectionParams = new OkapiConnectionParams(headers);
  }

  @Test
  void shouldRetrieveRulesFromModConfig() {

    //when
    List<Rule> ruleList = configurationsClient.getRulesFromConfiguration(JOB_EXECUTION_ID, okapiConnectionParams);

    Assert.assertFalse(ruleList.isEmpty());
  }

  @Test
  void shouldRetrieveRulesFromModConfigThatEqualsToDefault() {

    // when
    List<Rule> ruleList = configurationsClient.getRulesFromConfiguration(JOB_EXECUTION_ID, okapiConnectionParams);

    // then
    Assert.assertEquals(DEFAULT_LEADER_FIELD_NAME, ruleList.get(0).getField());
    Assert.assertEquals(DEFAULT_LEADER_FIELD_DESCRIPTION, ruleList.get(0).getDescription());
    Assert.assertEquals(DEFAULT_LEADER_TRANSLATION_FUNCTION, ruleList.get(0).getDataSources().get(0).getTranslation().getFunction());
    Assert.assertEquals(DEFAULT_LEADER_TRANSLATION_PARAMETERS_POS17, ruleList.get(0).getDataSources().get(0).getTranslation().getParameter("position17"));
    Assert.assertEquals(DEFAULT_LEADER_TRANSLATION_PARAMETERS_POS18, ruleList.get(0).getDataSources().get(0).getTranslation().getParameter("position18"));
    Assert.assertEquals(DEFAULT_LEADER_TRANSLATION_PARAMETERS_POS19, ruleList.get(0).getDataSources().get(0).getTranslation().getParameter("position19"));
  }

  @Test
  void shouldRetrieveRecordLinkFromModConfig() {
    // given
    String id = UUID.randomUUID().toString();
    String expectedLink = "https://folio-testing.dev.folio.org/inventory/view/" + id;

    // when
    String recordLink = configurationsClient.getInventoryRecordLink(id, JOB_EXECUTION_ID, okapiConnectionParams);

    // then
    Assert.assertEquals(expectedLink, recordLink);

  }

  @Test
  void shouldReturnEmptyWhenResponseIsFail(VertxTestContext context) {

    //when
    Optional<JsonObject> recordLink = configurationsClient.getConfigsFromModConfigByQuery(JOB_EXECUTION_ID, "FAIL", okapiConnectionParams);

    // then
    vertx.setTimer(2000L, handler ->
      context.verify(() -> {
        Assert.assertTrue(recordLink.isEmpty());
        Criterion criterion = HelperUtils.getErrorLogCriterionByJobExecutionIdAndReason(JOB_EXECUTION_ID, "Error while query the configs from mod configuration by query");
        errorLogService.getByQuery(criterion, okapiConnectionParams.getTenantId())
          .onSuccess(errorLogList -> {
            Assertions.assertNotNull(errorLogList);
            Assertions.assertFalse(errorLogList.isEmpty());
            Assertions.assertEquals(JOB_EXECUTION_ID, errorLogList.get(0).getJobExecutionId());
            context.completeNow();
          });
      }));

  }

}
