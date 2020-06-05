package org.folio.service.mapping.processor;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.service.mapping.processor.rule.DataSource;
import org.folio.service.mapping.processor.rule.Rule;
import org.folio.service.mapping.processor.translations.Translation;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Boolean.TRUE;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.folio.rest.jaxrs.model.RecordType.HOLDINGS;

public class RuleFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String DEFAULT_RULES_PATH = "rules/rulesDefault.json";

  private static final String SET_VALUE_FUNCTION = "set_value";
  private static final String VALUE_PARAMETER = "value";
  private static final String INDICATOR_NAME_1 = "1";
  private static final String INDICATOR_NAME_2 = "2";
  private static final String SUBFIELD_REGEX = "(?<=\\$).{1}";
  private static final String TEMPORARY_LOCATION_FIELD_ID = "temporaryLocationId";

  private static final Map<String, String> translationFunctions = ImmutableMap.of(
    "permanentLocationId", "set_location",
    "temporaryLocationId", "set_location",
    "effectiveLocationId", "set_location"
  );
  private static final String PERMANENT_LOCATION_FIELD_ID = "permanentLocationId";

  private List<Rule> defaultRules;

  public List<Rule> create(MappingProfile mappingProfile) {
    if (mappingProfile == null || isEmpty(mappingProfile.getTransformations())) {
      return getDefaultRules();
    }
    List<Rule> rules = Lists.newArrayList(getDefaultRules());
    rules.addAll(buildByTransformations(mappingProfile.getTransformations()));
    return rules;
  }

  protected List<Rule> getDefaultRules() {
    if (Objects.nonNull(this.defaultRules)) {
      return this.defaultRules;
    }
    URL url = Resources.getResource(DEFAULT_RULES_PATH);
    String stringRules = null;
    try {
      stringRules = Resources.toString(url, StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOGGER.error("Failed to fetch default rules for export");
      throw new NotFoundException(e);
    }
    this.defaultRules = Arrays.asList(Json.decodeValue(stringRules, Rule[].class));
    return this.defaultRules;
  }

  private List<Rule> buildByTransformations(List<Transformations> mappingTransformations) {
    List<Rule> rules = new ArrayList<>();
    String temporaryLocationTransformation = getTemporaryLocationTransformation(mappingTransformations);
    for (Transformations mappingTransformation : mappingTransformations) {
      if (TRUE.equals(mappingTransformation.getEnabled()) && isNotBlank(mappingTransformation.getPath())
        && isNotBlank(mappingTransformation.getTransformation())) {
        if (!(isHoldingsPermanentLocation(mappingTransformation)
          && temporaryLocationTransformation.equals(mappingTransformation.getTransformation()))) {
          rules.add(buildByTransformation(mappingTransformation));
        }
      }
    }
    return rules;
  }

  private String getTemporaryLocationTransformation(List<Transformations> mappingTransformations) {
    Optional<Transformations> temporaryLocationTransformation = mappingTransformations.stream()
      .filter(transformations -> HOLDINGS.equals(transformations.getRecordType()))
      .filter(transformations -> TEMPORARY_LOCATION_FIELD_ID.equals(transformations.getFieldId()))
      .findFirst();
    if (temporaryLocationTransformation.isPresent()) {
      return temporaryLocationTransformation.get().getTransformation();
    }
    return StringUtils.EMPTY;
  }

  private boolean isHoldingsPermanentLocation(Transformations mappingTransformation) {
    return HOLDINGS.equals(mappingTransformation.getRecordType()) && PERMANENT_LOCATION_FIELD_ID.equals(mappingTransformation.getFieldId());
  }


  private Rule buildByTransformation(Transformations mappingTransformation) {
    String field = substring(mappingTransformation.getTransformation(), 0, 3);
    Rule rule = new Rule();
    rule.setField(field);
    rule.setDataSources(buildDataSources(mappingTransformation));
    return rule;
  }

  private List<DataSource> buildDataSources(Transformations mappingTransformation) {
    List<DataSource> dataSources = new ArrayList<>();
    DataSource fromDataSource = new DataSource();
    fromDataSource.setFrom(mappingTransformation.getPath());
    if (translationFunctions.containsKey(mappingTransformation.getFieldId())) {
      Translation translation = new Translation();
      translation.setFunction(translationFunctions.get(mappingTransformation.getFieldId()));
      fromDataSource.setTranslation(translation);
    }
    dataSources.add(fromDataSource);
    String transformation = mappingTransformation.getTransformation();
    Pattern pattern = Pattern.compile(SUBFIELD_REGEX);
    Matcher matcher = pattern.matcher(transformation);
    if (matcher.find()) {
      fromDataSource.setSubfield(matcher.group());
      String indicator1 = substring(mappingTransformation.getTransformation(), 3, 4);
      String indicator2 = substring(mappingTransformation.getTransformation(), 4, 5);
      dataSources.add(buildIndicatorDataSource(INDICATOR_NAME_1, indicator1));
      dataSources.add(buildIndicatorDataSource(INDICATOR_NAME_2, indicator2));
    }
    return dataSources;
  }


  private DataSource buildIndicatorDataSource(String indicatorName, String indicatorValue) {
    Translation indicatorTranslation = new Translation();
    indicatorTranslation.setFunction(SET_VALUE_FUNCTION);
    Map<String, String> parameters = new HashMap<>();
    parameters.put(VALUE_PARAMETER, indicatorValue);
    DataSource indicatorDataSource = new DataSource();
    indicatorTranslation.setParameters(parameters);
    indicatorDataSource.setTranslation(indicatorTranslation);
    indicatorDataSource.setIndicator(indicatorName);
    return indicatorDataSource;
  }

}
