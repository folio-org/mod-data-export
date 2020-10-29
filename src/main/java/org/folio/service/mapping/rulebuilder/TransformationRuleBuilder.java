package org.folio.service.mapping.rulebuilder;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.folio.processor.rule.DataSource;
import org.folio.processor.rule.Rule;
import org.folio.processor.translations.Translation;
import org.folio.rest.jaxrs.model.Transformations;
import org.folio.service.mapping.translationbuilder.DefaultTranslationBuilder;
import org.folio.service.mapping.translationbuilder.LocationTranslationBuilder;
import org.folio.service.mapping.translationbuilder.TranslationBuilder;
import org.folio.service.transformationfields.TransformationFieldsConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Comparator.comparing;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.apache.commons.lang3.StringUtils.substring;

public class TransformationRuleBuilder implements RuleBuilder {

  private static final Comparator<String> SUBFIELD_COMPARATOR = Comparator.nullsLast((subField0, subField1) -> {
    // Objects with not empty subfield value should be at the top of the sorted list.
    // If the DataSource contains numeric subfields, it will follow the alphabetical subfields.
    if (isNumeric(subField0) == isNumeric(subField1)) {
      return subField0.compareTo(subField1);
    }
    return isNumeric(subField0) && !isNumeric(subField1) ? 1 : -1;
  });

  private static final String SET_VALUE_FUNCTION = "set_value";
  private static final String VALUE_PARAMETER = "value";
  private static final String INDICATOR_NAME_1 = "1";
  private static final String INDICATOR_NAME_2 = "2";
  private static final String SUBFIELD_REGEX = "(?<=\\$).{1}";
  private static final String SET_METADATA_UPDATED_DATE_FIELD_ID = "metadata.updateddate";
  private static final String SET_METADATA_CREATED_DATE_FIELD_ID = "metadata.createddate";
  private static final String MATERIAL_TYPE_FIELD_ID = "materialtypeid";
  private static final String PERMANENT_LOAN_TYPE_FIELD_ID = "permanentloantypeid";
  private static final String INSTANCE_TYPE_FIELD_ID = "instancetypeid";
  private static final String MOD_OF_ISSUANCE_ID = "modeofissuanceid";
  private static final String PERMANENT_LOCATION_NAME = "permanentlocation";
  private static final String TEMPORARY_LOCATION_NAME = "temporarylocation";
  private static final String EFFECTIVE_LOCATION_NAME = "effectivelocation";
  private static final String SET_LOCATION_FUNCTION = "set_location";
  private static final String SET_MATERIAL_TYPE_FUNCTION = "set_material_type";
  private static final String SET_LOAN_TYPE_FUNCTION = "set_loan_type";
  private static final String SET_INSTANCE_TYPE_ID_FUNCTION = "set_instance_type_id";
  private static final String SET_CALL_NUMBER_TYPE_ID_FUNCTION = "set_call_number_type_id";
  private static final String SET_METADATA_DATE_TIME_FUNCTION = "set_metadata_date_time";
  private static final String MOD_OF_ISSUANCE_ID_FUNCTION = "set_mode_of_issuance_id";
  private static final String DEFAULT = "default";

  private static final Map<String, String> translationFunctions = ImmutableMap.<String, String>builder()
    .put(MATERIAL_TYPE_FIELD_ID, SET_MATERIAL_TYPE_FUNCTION)
    .put(PERMANENT_LOAN_TYPE_FIELD_ID, SET_LOAN_TYPE_FUNCTION)
    .put(INSTANCE_TYPE_FIELD_ID, SET_INSTANCE_TYPE_ID_FUNCTION)
    .put(SET_METADATA_UPDATED_DATE_FIELD_ID, SET_METADATA_DATE_TIME_FUNCTION)
    .put(SET_METADATA_CREATED_DATE_FIELD_ID, SET_METADATA_DATE_TIME_FUNCTION)
    .put(TransformationFieldsConfig.HOLDINGS_CALL_NUMBER_TYPE.getFieldId().toLowerCase(), SET_CALL_NUMBER_TYPE_ID_FUNCTION)
    .put(MOD_OF_ISSUANCE_ID, MOD_OF_ISSUANCE_ID_FUNCTION)
    .put(PERMANENT_LOCATION_NAME, SET_LOCATION_FUNCTION)
    .put(TEMPORARY_LOCATION_NAME, SET_LOCATION_FUNCTION)
    .put(EFFECTIVE_LOCATION_NAME, SET_LOCATION_FUNCTION)
    .build();

  private static final Map<String, TranslationBuilder> translationBuilders = ImmutableMap.<String, TranslationBuilder>builder()
    .put(PERMANENT_LOCATION_NAME, new LocationTranslationBuilder())
    .put(TEMPORARY_LOCATION_NAME, new LocationTranslationBuilder())
    .put(EFFECTIVE_LOCATION_NAME, new LocationTranslationBuilder())
    .put(DEFAULT, new DefaultTranslationBuilder())
    .build();

  @Override
  public Optional<Rule> build(Collection<Rule> rules, Transformations mappingTransformation) {
    String field = substring(mappingTransformation.getTransformation(), 0, 3);
    Rule rule;
    Optional<Rule> existingRule = rules.stream()
      .filter(tagRule -> tagRule.getField()
        .equals(field))
      .findFirst();
    //If there is already an existing rule, then just append the subfield, without indicators
    if (existingRule.isPresent()) {
      rule = existingRule.get();
      rule.setHasSameTagInItems(mappingTransformation.getHasSameTagInItems());
      rule.getDataSources().addAll(buildDataSources(mappingTransformation, false));
    } else {
      rule = new Rule();
      rule.setField(field);
      setDataSources(rule, mappingTransformation);
    }
    if (MapUtils.isNotEmpty(mappingTransformation.getMetadataParameters())) {
      Map<String, String> metadata = new HashMap<>();
      for (Map.Entry<String, String> metadataParameter : mappingTransformation.getMetadataParameters().entrySet()) {
        metadata.put(metadataParameter.getKey(), metadataParameter.getValue());
      }
      rule.setMetadata(metadata);
    }
    rule.getDataSources().sort(comparing(DataSource::getSubfield, SUBFIELD_COMPARATOR));
    return Optional.of(rule);
  }

  private List<DataSource> buildDataSources(Transformations mappingTransformation, boolean setIndicators) {
    List<DataSource> dataSources = new ArrayList<>();
    DataSource fromDataSource = new DataSource();
    fromDataSource.setFrom(mappingTransformation.getPath());
    buildTranslation(mappingTransformation, fromDataSource);
    dataSources.add(fromDataSource);
    String transformation = mappingTransformation.getTransformation();
    Pattern pattern = Pattern.compile(SUBFIELD_REGEX);
    Matcher matcher = pattern.matcher(transformation);
    if (matcher.find()) {
      fromDataSource.setSubfield(matcher.group());
      //set indicator fields only for a unique rule
      if (setIndicators) {
        String indicator1 = substring(mappingTransformation.getTransformation(), 3, 4);
        String indicator2 = substring(mappingTransformation.getTransformation(), 4, 5);
        dataSources.add(buildIndicatorDataSource(INDICATOR_NAME_1, indicator1));
        dataSources.add(buildIndicatorDataSource(INDICATOR_NAME_2, indicator2));
      }
    }
    return dataSources;
  }

  private void buildTranslation(Transformations mappingTransformation, DataSource fromDataSource) {
    String fieldId = mappingTransformation.getFieldId();
    translationFunctions.forEach((key, value) -> {
      if (isNotEmpty(fieldId) && fieldId.contains(key)) {
        Translation translation;
        List<String> fieldParts = Splitter.on(".").splitToList(mappingTransformation.getFieldId());
        if (fieldParts.size() > 1 && translationBuilders.containsKey(fieldParts.get(1))) {
          translation = translationBuilders.get(fieldParts.get(1)).build(value, mappingTransformation);
        } else {
          translation = translationBuilders.get(DEFAULT).build(value, mappingTransformation);
        }
        fromDataSource.setTranslation(translation);
      }
    });
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

  private void setDataSources(Rule rule, Transformations mappingTransformation) {
    if (CollectionUtils.isNotEmpty(rule.getDataSources())) {
      rule.getDataSources().addAll(buildDataSources(mappingTransformation, true));
    } else {
      rule.setDataSources(buildDataSources(mappingTransformation, true));
    }
  }

}
