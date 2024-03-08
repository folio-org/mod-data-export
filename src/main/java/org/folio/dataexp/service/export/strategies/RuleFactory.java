package org.folio.dataexp.service.export.strategies;

import com.google.common.collect.ImmutableMap;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.folio.dataexp.domain.dto.Transformations;
import org.folio.dataexp.exception.TransformationRuleException;
import org.folio.dataexp.service.export.strategies.rule.builder.CombinedRuleBuilder;
import org.folio.dataexp.service.export.strategies.rule.builder.DefaultRuleBuilder;
import org.folio.dataexp.service.export.strategies.rule.builder.RuleBuilder;
import org.folio.dataexp.service.export.strategies.rule.builder.TransformationRuleBuilder;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.processor.rule.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.folio.dataexp.service.export.Constants.DEFAULT_INSTANCE_MAPPING_PROFILE_ID;

@Log4j2
@Component
public class RuleFactory {

  private static final String TEMPORARY_LOCATION_FIELD_ID = "holdings.temporarylocation.name";
  private static final String PERMANENT_LOCATION_FIELD_ID = "holdings.permanentlocation.name";
  private static final String DEFAULT_BUILDER_KEY = "default.builder";
  private static final String TRANSFORMATION_BUILDER_KEY = "transformation.builder";
  private static final String INSTANCE_ELECTRONIC_ACCESS_ID = "instance.electronic.access";

  private static final Map<String, RuleBuilder> ruleBuilders = ImmutableMap.<String, RuleBuilder>builder()
    .put(INSTANCE_ELECTRONIC_ACCESS_ID, new CombinedRuleBuilder(3, INSTANCE_ELECTRONIC_ACCESS_ID))
    .put(TRANSFORMATION_BUILDER_KEY, new TransformationRuleBuilder())
    .put(DEFAULT_BUILDER_KEY, new DefaultRuleBuilder())
    .build();

  private final List<Rule> defaultRulesFromConfigFile;
  private final List<Rule> defaultHoldingsRulesFromConfigFile;

  private ErrorLogService errorLogService;

  @Autowired
  public RuleFactory(List<Rule> defaultRulesFromConfigFile, List<Rule> defaultHoldingsRulesFromConfigFile) {
    this.defaultRulesFromConfigFile = defaultRulesFromConfigFile;
    this.defaultHoldingsRulesFromConfigFile = defaultHoldingsRulesFromConfigFile;
  }


  public List<Rule> getRules(MappingProfile mappingProfile) throws TransformationRuleException {
    if (mappingProfile != null && !mappingProfile.getRecordTypes().contains(RecordTypes.INSTANCE)) {
      return create(mappingProfile);
    }
    //ToDo MDEXP-673
    List<Rule> rulesFromConfig = new ArrayList<>();
    if (mappingProfile != null && isNotEmpty(rulesFromConfig)) {
      log.info("Using overridden rules configuration with transformations from the mapping profile with id {}", mappingProfile.getId());
    }
    return CollectionUtils.isEmpty(rulesFromConfig) ? create(mappingProfile) : create(mappingProfile, rulesFromConfig, true);
  }

  public List<Rule> create(MappingProfile mappingProfile) throws TransformationRuleException {
    return createRulesDependsOnRecordType(mappingProfile);
  }

  public List<Rule> create(MappingProfile mappingProfile, List<Rule> defaultRules, boolean appendDefaultHoldingsRules) throws TransformationRuleException {
    if (appendDefaultHoldingsRules && mappingProfile != null && mappingProfile.getRecordTypes().contains(RecordTypes.HOLDINGS)) {
      defaultRules.addAll(defaultHoldingsRulesFromConfigFile);
    }
    if (mappingProfile == null || CollectionUtils.isEmpty(mappingProfile.getTransformations())) {
      log.info("No Mapping rules specified, using default mapping rules");
      return defaultRules;
    }
    List<Rule> rules = new ArrayList<>(createByTransformations(mappingProfile.getTransformations(), defaultRules));
    if (isDefaultInstanceProfile(mappingProfile.getId()) && isNotEmpty(mappingProfile.getTransformations())) {
      rules.addAll(defaultRulesFromConfigFile);
    }
    return rules;
  }

  public Set<Rule> createByTransformations(List<Transformations> mappingTransformations, List<Rule> defaultRules) throws TransformationRuleException {
    Set<Rule> rules = new LinkedHashSet<>();
    String temporaryLocationTransformation = getTemporaryLocationTransformation(mappingTransformations);
    Optional<Rule> rule = Optional.empty();
    for (Transformations mappingTransformation : mappingTransformations) {
      if (isTransformationValidAndNotBlank(mappingTransformation)
        && isPermanentLocationNotEqualsTemporaryLocation(temporaryLocationTransformation, mappingTransformation)) {
        rule = ruleBuilders.get(TRANSFORMATION_BUILDER_KEY).build(rules, mappingTransformation, errorLogService);
      } else if (isInstanceTransformationValidAndBlank(mappingTransformation) || isHoldingsTransformationValidAndBlank(mappingTransformation)) {
        rule = createDefaultByTransformations(mappingTransformation, defaultRules);
      } else if (RecordTypes.ITEM.equals(mappingTransformation.getRecordType())) {
        log.error(String.format("No transformation provided for field name: %s, and with record type: %s",
          mappingTransformation.getFieldId(), mappingTransformation.getRecordType()));
      }
      if (rule.isPresent()) {
        rules.add(rule.get());
      }
    }
    return rules;
  }

  public Optional<Rule> createDefaultByTransformations(Transformations mappingTransformation, List<Rule> defaultRules) throws TransformationRuleException {
    RecordTypes recordType = mappingTransformation.getRecordType();
    if (TRUE.equals(mappingTransformation.getEnabled()) && StringUtils.isNotBlank(mappingTransformation.getFieldId())
      && RecordTypes.INSTANCE.equals(recordType)) {
      for (Map.Entry<String, RuleBuilder> ruleBuilderEntry : ruleBuilders.entrySet()) {
        if (mappingTransformation.getFieldId().contains(ruleBuilderEntry.getKey())) {
          return ruleBuilderEntry.getValue().build(defaultRules, mappingTransformation, errorLogService);
        }
      }
      return ruleBuilders.get(DEFAULT_BUILDER_KEY).build(defaultRules, mappingTransformation, errorLogService);
    }
    return Optional.empty();
  }

  public static boolean isDefaultInstanceProfile(UUID mappingProfileId) {
    return DEFAULT_INSTANCE_MAPPING_PROFILE_ID.equals(mappingProfileId.toString());
  }

  private boolean isTransformationValidAndNotBlank(Transformations mappingTransformation) {
    return isTransformationValid(mappingTransformation) && StringUtils.isNotBlank(mappingTransformation.getTransformation());
  }

  private boolean isPermanentLocationNotEqualsTemporaryLocation(String temporaryLocationTransformation, Transformations mappingTransformation) {
    return !(isHoldingsPermanentLocation(mappingTransformation) && temporaryLocationTransformation.equals(mappingTransformation.getTransformation()));
  }

  private boolean isInstanceTransformationValidAndBlank(Transformations mappingTransformation) {
    return isTransformationValid(mappingTransformation) && RecordTypes.INSTANCE.equals(mappingTransformation.getRecordType()) && StringUtils.isBlank(mappingTransformation.getTransformation());
  }

  private boolean isHoldingsTransformationValidAndBlank(Transformations mappingTransformation) {
    return isTransformationValid(mappingTransformation) && RecordTypes.HOLDINGS.equals(mappingTransformation.getRecordType()) && StringUtils.isBlank(mappingTransformation.getTransformation());
  }

  private String getTemporaryLocationTransformation(List<Transformations> mappingTransformations) {
    Optional<Transformations> temporaryLocationTransformation = mappingTransformations.stream()
      .filter(transformations -> RecordTypes.HOLDINGS.equals(transformations.getRecordType()))
      .filter(transformations -> TEMPORARY_LOCATION_FIELD_ID.equals(transformations.getFieldId()))
      .findFirst();
    if (temporaryLocationTransformation.isPresent()) {
      return temporaryLocationTransformation.get().getTransformation();
    }
    return StringUtils.EMPTY;
  }

  private boolean isHoldingsPermanentLocation(Transformations mappingTransformation) {
    return RecordTypes.HOLDINGS.equals(mappingTransformation.getRecordType()) && PERMANENT_LOCATION_FIELD_ID.equals(mappingTransformation.getFieldId());
  }

  private boolean isTransformationValid(Transformations mappingTransformation) {
    return Boolean.TRUE.equals(mappingTransformation.getEnabled()) && StringUtils.isNotBlank(mappingTransformation.getPath());
  }

  private List<Rule> createRulesDependsOnRecordType(MappingProfile mappingProfile) throws TransformationRuleException {
    List<Rule> combinedDefaultRules = new ArrayList<>();
    if (mappingProfile == null || mappingProfile.getRecordTypes().contains(RecordTypes.INSTANCE)) {
      combinedDefaultRules.addAll(defaultRulesFromConfigFile);
    }
    if (mappingProfile != null && (mappingProfile.getRecordTypes().contains(RecordTypes.HOLDINGS))) {
      combinedDefaultRules.addAll(defaultHoldingsRulesFromConfigFile);
    }
    return create(mappingProfile, combinedDefaultRules, false);
  }

  private void validateRules() {

  }

  @Autowired
  private void setErrorLogService(ErrorLogService errorLogService) {
    this.errorLogService = errorLogService;
  }

}
