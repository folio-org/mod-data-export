package org.folio.clients;

import com.google.common.io.Resources;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.collections4.map.HashedMap;
import org.folio.rest.RestVerticleTestBase;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.service.mapping.processor.rule.Rule;
import org.folio.util.OkapiConnectionParams;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.jaxrs.model.RecordType.HOLDINGS;

@RunWith(VertxUnitRunner.class)
class ConfigurationsClientUnitTest extends RestVerticleTestBase {
  private static final String DEFAULT_LEADER_FIELD_NAME = "leader";
  private static final String DEFAULT_LEADER_FIELD_DESCRIPTION = "Leader";
  private static final String DEFAULT_LEADER_TRANSLATION_FUNCTION = "set_17-19_positions";
  private static final String DEFAULT_LEADER_TRANSLATION_PARAMETERS_POS17 = "3";
  private static final String DEFAULT_LEADER_TRANSLATION_PARAMETERS_POS18 = "c";
  private static final String DEFAULT_LEADER_TRANSLATION_PARAMETERS_POS19 = " ";
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
    List<Rule> ruleList = configurationsClient.getRulesFromConfiguration(new MappingProfile(), okapiConnectionParams);

    Assert.assertFalse(ruleList.isEmpty());
  }

  @Test
  void shouldRetrieveRulesFromModConfigThatEqualsToDefault() throws IOException {
    ConfigurationsClient configurationsClient = new ConfigurationsClient();

    //when
    List<Rule> ruleList = configurationsClient.getRulesFromConfiguration(new MappingProfile(), okapiConnectionParams);

    Assert.assertEquals(DEFAULT_LEADER_FIELD_NAME, ruleList.get(0).getField());
    Assert.assertEquals(DEFAULT_LEADER_FIELD_DESCRIPTION, ruleList.get(0).getDescription());
    Assert.assertEquals(DEFAULT_LEADER_TRANSLATION_FUNCTION, ruleList.get(0).getDataSources().get(0).getTranslation().getFunction());
    Assert.assertEquals(DEFAULT_LEADER_TRANSLATION_PARAMETERS_POS17, ruleList.get(0).getDataSources().get(0).getTranslation().getParameter("position17"));
    Assert.assertEquals(DEFAULT_LEADER_TRANSLATION_PARAMETERS_POS18, ruleList.get(0).getDataSources().get(0).getTranslation().getParameter("position18"));
    Assert.assertEquals(DEFAULT_LEADER_TRANSLATION_PARAMETERS_POS19, ruleList.get(0).getDataSources().get(0).getTranslation().getParameter("position19"));
    Assert.assertEquals(getDefaultRules().size(), ruleList.size());
  }

  @Test
  void shouldRetrieveRulesFromModConfigAndAppendRulesFromMappingProfile() throws IOException {
    ConfigurationsClient configurationsClient = new ConfigurationsClient();
    List<Transformations> transformations = new ArrayList<>();
    transformations.add(new Transformations()
      .withEnabled(true)
      .withRecordType(HOLDINGS)
      .withPath("$.holdings[*].callNumber")
      .withFieldId("callNumber")
      .withTransformation("900ff$a"));
    MappingProfile mappingProfile = new MappingProfile()
      .withTransformations(transformations);
    //when
    List<Rule> ruleList = configurationsClient.getRulesFromConfiguration(mappingProfile, okapiConnectionParams);

    Assert.assertEquals(getDefaultRules().size() + 1, ruleList.size());
  }


  private List<Rule> getDefaultRules() throws IOException {
    URL url = Resources.getResource("rules/rulesDefault.json");
    String stringRules = Resources.toString(url, StandardCharsets.UTF_8);
    return Arrays.asList(Json.decodeValue(stringRules, Rule[].class));
  }

}
