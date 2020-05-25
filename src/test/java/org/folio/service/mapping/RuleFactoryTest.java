package org.folio.service.mapping;

import io.vertx.core.json.Json;
import org.folio.TestUtil;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.service.mapping.processor.RuleFactory;
import org.folio.service.mapping.processor.rule.Rule;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
class RuleFactoryTest {
  private static final String DEFAULT_MAPPING_PROFILE_ID = "25d81cbe-9686-11ea-bb37-0242ac130002";
  private static final String DEFAULT_RULES_PATH = "rules/rulesDefault.json";
  private static List<Rule> defaultRules;

  private static RuleFactory ruleFactory;

  @BeforeAll
  static void setup() {
    defaultRules = Arrays.asList(Json.decodeValue(TestUtil.readFileContentFromResources(DEFAULT_RULES_PATH), Rule[].class));
    ruleFactory = new RuleFactory();
  }

  @Test
  void shouldReturnDefaultRules_whenMappingProfileIsNull() {
    // when
    List<Rule> rules = ruleFactory.create(null);

    // then
    Assert.assertEquals(rules.size(), defaultRules.size());
    Assert.assertEquals(rules.get(0).getField(), defaultRules.get(0).getField());
    Assert.assertEquals(rules.get(0).getDescription(), defaultRules.get(0).getDescription());
  }

  @Test
  void shouldReturnDefaultRules_whenMappingProfileIsDefault() {
    // given
    MappingProfile mappingProfile = new MappingProfile()
      .withId(DEFAULT_MAPPING_PROFILE_ID);

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    Assert.assertEquals(rules.size(), defaultRules.size());
    Assert.assertEquals(rules.get(0).getField(), defaultRules.get(0).getField());
    Assert.assertEquals(rules.get(0).getDescription(), defaultRules.get(0).getDescription());
  }

  @Test
  void shouldReturnDefaultRules_whenMappingProfileTransformationsIsEmpty() {
    // given
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString());

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    Assert.assertEquals(rules.size(), defaultRules.size());
    Assert.assertEquals(rules.get(0).getField(), defaultRules.get(0).getField());
    Assert.assertEquals(rules.get(0).getDescription(), defaultRules.get(0).getDescription());
  }

}
