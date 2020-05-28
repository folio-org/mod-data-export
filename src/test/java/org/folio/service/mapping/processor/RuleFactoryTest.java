package org.folio.service.mapping.processor;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.service.mapping.processor.rule.DataSource;
import org.folio.service.mapping.processor.rule.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.mockito.Mockito.doReturn;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
class RuleFactoryTest {
  private static final String DEFAULT_RULE_FIELD_VALUE = "001";
  private static final String DEFAULT_RULE_DESCRIPTION = "defaultRuleDescription";
  private static final String DEFAULT_RULE_FROM_VALUE = "defaultFromValue";
  private static final String TRANSFORMATIONS_PATH_1 = "transformationsPath1";
  private static final String TRANSFORMATION_FIELD_VALUE_1 = "002";
  private static final String TRANSFORMATION_FIELD_VALUE_WITH_SUBFIELD = "002 $a";
  private static final String TRANSFORMATIONS_PATH_2 = "transformationsPath2";
  private static final String TRANSFORMATION_FIELD_VALUE_2 = "003";
  private static final String SUBFIELD_A = "a";
  private static final String FIRST_INDICATOR = "1";
  private static final String SET_VALUE_FUNCTION = "set_value";
  private static final String VALUE_PARAMETER = "value";
  private static final String SECOND_INDICATOR = "2";

  @InjectMocks
  @Spy
  private static RuleFactory ruleFactory = new RuleFactory();

  private static List<Rule> defaultRules;

  @BeforeEach
  public void setUp() {
    DataSource dataSource = new DataSource();
    dataSource.setFrom(DEFAULT_RULE_FROM_VALUE);
    Rule defaultRule = new Rule();
    defaultRule.setField(DEFAULT_RULE_FIELD_VALUE);
    defaultRule.setDescription(DEFAULT_RULE_DESCRIPTION);
    defaultRule.setDataSources(Lists.newArrayList(dataSource));
    defaultRules = Lists.newArrayList(defaultRule);

    doReturn(defaultRules).when(ruleFactory).getDefaultRules();
  }

  @Test
  void shouldReturnDefaultRules_whenMappingProfileIsNull() {
    // when
    List<Rule> rules = ruleFactory.create(null);

    // then
    assertEquals(rules.size(), 1);
    assertEquals(rules.get(0).getField(), DEFAULT_RULE_FIELD_VALUE);
    assertEquals(rules.get(0).getDescription(), DEFAULT_RULE_DESCRIPTION);
    assertEquals(rules.get(0).getDataSources().get(0).getFrom(), DEFAULT_RULE_FROM_VALUE);
  }

  @Test
  void shouldReturnDefaultRules_whenMappingProfileTransformationsIsEmpty() {
    // given
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString());

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(rules.size(), 1);
    assertEquals(rules.get(0).getField(), DEFAULT_RULE_FIELD_VALUE);
    assertEquals(rules.get(0).getDescription(), DEFAULT_RULE_DESCRIPTION);
    assertEquals(rules.get(0).getDataSources().get(0).getFrom(), DEFAULT_RULE_FROM_VALUE);
  }

  @Test
  void shouldReturnDefaultRules_whenMappingProfileTransformationsIsNotEnabled() {
    // given
    Transformations transformations = new Transformations()
      .withEnabled(false);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(ImmutableList.of(transformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(rules.size(), 1);
    assertEquals(rules.get(0).getField(), DEFAULT_RULE_FIELD_VALUE);
    assertEquals(rules.get(0).getDescription(), DEFAULT_RULE_DESCRIPTION);
    assertEquals(rules.get(0).getDataSources().get(0).getFrom(), DEFAULT_RULE_FROM_VALUE);
  }

  @Test
  void shouldReturnDefaultRules_whenMappingProfileTransformationsPathIsEmpty() {
    // given
    Transformations transformations = new Transformations()
      .withEnabled(true)
      .withPath(EMPTY);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(ImmutableList.of(transformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(rules.size(), 1);
    assertEquals(rules.get(0).getField(), DEFAULT_RULE_FIELD_VALUE);
    assertEquals(rules.get(0).getDescription(), DEFAULT_RULE_DESCRIPTION);
    assertEquals(rules.get(0).getDataSources().get(0).getFrom(), DEFAULT_RULE_FROM_VALUE);
  }

  @Test
  void shouldReturnDefaultRules_whenMappingProfileTransformationsValueIsEmpty() {
    // given
    Transformations transformations = new Transformations()
      .withEnabled(true)
      .withPath(TRANSFORMATIONS_PATH_1)
      .withTransformation(EMPTY);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(ImmutableList.of(transformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(rules.size(), 1);
    assertEquals(rules.get(0).getField(), DEFAULT_RULE_FIELD_VALUE);
    assertEquals(rules.get(0).getDescription(), DEFAULT_RULE_DESCRIPTION);
    assertEquals(rules.get(0).getDataSources().get(0).getFrom(), DEFAULT_RULE_FROM_VALUE);
  }

  @Test
  void shouldReturnDefaultRulesWithOneTransformationRule_whenMappingProfileTransformationsContainsValueWithoutSubfield() {
    // given
    Transformations transformations = new Transformations()
      .withEnabled(true)
      .withPath(TRANSFORMATIONS_PATH_1)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(Lists.newArrayList(transformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(rules.size(), 2);
    assertEquals(rules.get(0).getField(), DEFAULT_RULE_FIELD_VALUE);
    assertEquals(rules.get(0).getDescription(), DEFAULT_RULE_DESCRIPTION);
    assertEquals(rules.get(0).getDataSources().get(0).getFrom(), DEFAULT_RULE_FROM_VALUE);
    assertEquals(rules.get(1).getField(), TRANSFORMATION_FIELD_VALUE_1);
    assertEquals(rules.get(1).getDataSources().get(0).getFrom(), TRANSFORMATIONS_PATH_1);
  }

  @Test
  void shouldReturnDefaultRulesWithTwoTransformationRules_whenMappingProfileTransformationsContainsValueWithoutSubfield() {
    // given
    Transformations transformations1 = new Transformations()
      .withEnabled(true)
      .withPath(TRANSFORMATIONS_PATH_1)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_1);
    Transformations transformations2 = new Transformations()
      .withEnabled(true)
      .withPath(TRANSFORMATIONS_PATH_2)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_2);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(Lists.newArrayList(transformations1, transformations2));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(rules.size(), 3);
    assertEquals(rules.get(0).getField(), DEFAULT_RULE_FIELD_VALUE);
    assertEquals(rules.get(0).getDescription(), DEFAULT_RULE_DESCRIPTION);
    assertEquals(rules.get(0).getDataSources().get(0).getFrom(), DEFAULT_RULE_FROM_VALUE);
    assertEquals(rules.get(1).getField(), TRANSFORMATION_FIELD_VALUE_1);
    assertEquals(rules.get(1).getDataSources().get(0).getFrom(), TRANSFORMATIONS_PATH_1);
    assertEquals(rules.get(2).getField(), TRANSFORMATION_FIELD_VALUE_2);
    assertEquals(rules.get(2).getDataSources().get(0).getFrom(), TRANSFORMATIONS_PATH_2);
  }

  @Test
  void shouldReturnDefaultRulesWithOneTransformationRule_whenMappingProfileTransformationsContainsValueWithSubfield() {
    // given
    Transformations transformations = new Transformations()
      .withEnabled(true)
      .withPath(TRANSFORMATIONS_PATH_1)
      .withTransformation(TRANSFORMATION_FIELD_VALUE_WITH_SUBFIELD);
    MappingProfile mappingProfile = new MappingProfile()
      .withId(UUID.randomUUID().toString())
      .withTransformations(Lists.newArrayList(transformations));

    // when
    List<Rule> rules = ruleFactory.create(mappingProfile);

    // then
    assertEquals(rules.size(), 2);
    assertEquals(rules.get(0).getField(), DEFAULT_RULE_FIELD_VALUE);
    assertEquals(rules.get(0).getDescription(), DEFAULT_RULE_DESCRIPTION);
    assertEquals(rules.get(0).getDataSources().get(0).getFrom(), DEFAULT_RULE_FROM_VALUE);
    assertEquals(rules.get(1).getField(), TRANSFORMATION_FIELD_VALUE_1);
    assertEquals(rules.get(1).getDataSources().get(0).getFrom(), TRANSFORMATIONS_PATH_1);
    assertEquals(rules.get(1).getDataSources().get(0).getSubfield(), SUBFIELD_A);
    assertEquals(rules.get(1).getDataSources().get(1).getIndicator(), FIRST_INDICATOR);
    assertEquals(rules.get(1).getDataSources().get(1).getTranslation().getFunction(), SET_VALUE_FUNCTION);
    assertEquals(rules.get(1).getDataSources().get(1).getTranslation().getParameter(VALUE_PARAMETER), SPACE);
    assertEquals(rules.get(1).getDataSources().get(2).getIndicator(), SECOND_INDICATOR);
    assertEquals(rules.get(1).getDataSources().get(2).getTranslation().getFunction(), SET_VALUE_FUNCTION);
    assertEquals(rules.get(1).getDataSources().get(2).getTranslation().getParameter(VALUE_PARAMETER), SPACE);
  }

}
