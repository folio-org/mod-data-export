package org.folio.rest.impl;

import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.collections4.map.HashedMap;
import org.folio.clients.ConfigurationsClient;
import org.folio.processor.rule.Rule;
import org.folio.util.OkapiConnectionParams;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;

@RunWith(VertxUnitRunner.class)
class ConfigurationsClientTest extends RestVerticleTestBase {
  private static final String DEFAULT_LEADER_FIELD_NAME = "leader";
  private static final String DEFAULT_LEADER_FIELD_DESCRIPTION = "Leader";
  private static final String DEFAULT_LEADER_TRANSLATION_FUNCTION = "set_17-19_positions";
  private static final String DEFAULT_LEADER_TRANSLATION_PARAMETERS_POS17 = "3";
  private static final String DEFAULT_LEADER_TRANSLATION_PARAMETERS_POS18 = "c";
  private static final String DEFAULT_LEADER_TRANSLATION_PARAMETERS_POS19 = " ";
  private static final String JOB_EXECUTION_ID = UUID.randomUUID().toString();
  private static OkapiConnectionParams okapiConnectionParams;

  @BeforeAll
  public static void beforeClass() {
    Map<String, String> headers = new HashedMap<>();
    headers.put(OKAPI_HEADER_TENANT, TENANT_ID);
    headers.put(OKAPI_HEADER_URL, MOCK_OKAPI_URL);
    okapiConnectionParams = new OkapiConnectionParams(headers);
  }

  @Test
  void shouldRetrieveRulesFromModConfig() {
    ConfigurationsClient configurationsClient = new ConfigurationsClient();

    //when
    List<Rule> ruleList = configurationsClient.getRulesFromConfiguration(JOB_EXECUTION_ID, okapiConnectionParams);

    Assert.assertFalse(ruleList.isEmpty());
  }

  @Test
  void shouldRetrieveRulesFromModConfigThatEqualsToDefault() {
    ConfigurationsClient configurationsClient = new ConfigurationsClient();

    //when
    List<Rule> ruleList = configurationsClient.getRulesFromConfiguration(JOB_EXECUTION_ID, okapiConnectionParams);

    Assert.assertEquals(DEFAULT_LEADER_FIELD_NAME, ruleList.get(0).getField());
    Assert.assertEquals(DEFAULT_LEADER_FIELD_DESCRIPTION, ruleList.get(0).getDescription());
    Assert.assertEquals(DEFAULT_LEADER_TRANSLATION_FUNCTION, ruleList.get(0).getDataSources().get(0).getTranslation().getFunction());
    Assert.assertEquals(DEFAULT_LEADER_TRANSLATION_PARAMETERS_POS17, ruleList.get(0).getDataSources().get(0).getTranslation().getParameter("position17"));
    Assert.assertEquals(DEFAULT_LEADER_TRANSLATION_PARAMETERS_POS18, ruleList.get(0).getDataSources().get(0).getTranslation().getParameter("position18"));
    Assert.assertEquals(DEFAULT_LEADER_TRANSLATION_PARAMETERS_POS19, ruleList.get(0).getDataSources().get(0).getTranslation().getParameter("position19"));
  }

  @Test
  void shouldRetrieveRecordLinkFromModConfig() {
    ConfigurationsClient configurationsClient = new ConfigurationsClient();
    String id = UUID.randomUUID().toString();
    String expectedLink = "https://folio-testing.dev.folio.org/inventory/view/" + id;

    //when
    String recordLink = configurationsClient.getInventoryRecordLink(id, JOB_EXECUTION_ID, okapiConnectionParams);

    Assert.assertEquals(expectedLink, recordLink);

  }

}
