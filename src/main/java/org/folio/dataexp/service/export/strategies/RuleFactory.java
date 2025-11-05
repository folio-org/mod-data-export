package org.folio.dataexp.service.export.strategies;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.folio.dataexp.service.export.Constants.DEFAULT_INSTANCE_MAPPING_PROFILE_ID;
import static org.folio.dataexp.util.Constants.COMMA;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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

/**
 * Factory for building transformation rules for MARC export.
 */
@Log4j2
@Component
public class RuleFactory {

  private static final String TEMPORARY_LOCATION_FIELD_ID = "holdings.temporarylocation.name";
  private static final String PERMANENT_LOCATION_FIELD_ID = "holdings.permanentlocation.name";
  private static final String DEFAULT_BUILDER_KEY = "default.builder";
  private static final String TRANSFORMATION_BUILDER_KEY = "transformation.builder";
  private static final String INSTANCE_ELECTRONIC_ACCESS_ID = "instance.electronic.access";

  private static final Map<String, RuleBuilder> ruleBuilders =
      ImmutableMap.<String, RuleBuilder>builder()
        .put(INSTANCE_ELECTRONIC_ACCESS_ID, new CombinedRuleBuilder(3,
          INSTANCE_ELECTRONIC_ACCESS_ID))
        .put(TRANSFORMATION_BUILDER_KEY, new TransformationRuleBuilder())
        .put(DEFAULT_BUILDER_KEY, new DefaultRuleBuilder())
        .build();

  private final List<Rule> defaultRulesFromConfigFile;
  private final List<Rule> defaultHoldingsRulesFromConfigFile;

  @Autowired
  private ErrorLogService errorLogService;

  /**
   * Constructs a RuleFactory with the provided default rules.
   *
   * @param defaultRulesFromConfigFile         default rules for instance records
   * @param defaultHoldingsRulesFromConfigFile default rules for holdings records
   */
  @Autowired
  public RuleFactory(List<Rule> defaultRulesFromConfigFile,
      List<Rule> defaultHoldingsRulesFromConfigFile) {
    this.defaultRulesFromConfigFile = defaultRulesFromConfigFile;
    this.defaultHoldingsRulesFromConfigFile = defaultHoldingsRulesFromConfigFile;
  }

  /**
   * Gets rules for the given mapping profile.
   */
  public List<Rule> getRules(MappingProfile mappingProfile) throws TransformationRuleException {
    var rules = buildRules(mappingProfile);
    if (shouldSuppress999ff(mappingProfile)) {
      log.info("Suppressing 999ff");
      rules = rules.stream()
          .filter(rule ->
              !("999".equals(rule.getField()) && "ff".equals(fetchIndicators(rule)))
          )
          .toList();
    }
    if (isNotEmpty(mappingProfile.getFieldsSuppression())) {
      var fieldsToSuppress = Arrays.stream(
              mappingProfile.getFieldsSuppression().split(COMMA)
          )
          .map(StringUtils::trim)
          .toList();
      log.info(
          "Suppressing fields [{}]",
          String.join(COMMA, fieldsToSuppress)
      );
      return isEmpty(fieldsToSuppress)
          ? rules
          : rules.stream()
              .filter(rule -> !(fieldsToSuppress.contains(rule.getField())))
              .toList();
    }
    return rules;
  }

  private boolean shouldSuppress999ff(MappingProfile mappingProfile) {
    return !isNull(mappingProfile.getSuppress999ff()) && mappingProfile.getSuppress999ff();
  }

  private String fetchIndicators(Rule rule) {
    return rule.getDataSources().stream()
        .filter(dataSource -> nonNull(dataSource.getIndicator()))
        .map(dataSource -> dataSource.getTranslation().getParameter("value"))
        .collect(Collectors.joining());
  }

  /**
   * Builds rules for the given mapping profile.
   */
  public List<Rule> buildRules(MappingProfile mappingProfile)
      throws TransformationRuleException {
    if (mappingProfile != null
        && !mappingProfile.getRecordTypes().contains(RecordTypes.INSTANCE)) {
      return create(mappingProfile);
    }
    List<Rule> rulesFromConfig = new ArrayList<>();
    if (mappingProfile != null && isNotEmpty(rulesFromConfig)) {
      log.info(
          "Using overridden rules configuration with transformations from the mapping profile "
            + "with id {}",
          mappingProfile.getId()
      );
    }
    return CollectionUtils.isEmpty(rulesFromConfig)
        ? create(mappingProfile)
        : create(mappingProfile, rulesFromConfig, true);
  }

  /**
   * Creates rules for the given mapping profile.
   */
  public List<Rule> create(MappingProfile mappingProfile) throws TransformationRuleException {
    return createRulesDependsOnRecordType(mappingProfile);
  }

  /**
   * Creates rules for the given mapping profile and default rules.
   */
  public List<Rule> create(
      MappingProfile mappingProfile,
      List<Rule> defaultRules,
      boolean appendDefaultHoldingsRules
  ) throws TransformationRuleException {
    if (appendDefaultHoldingsRules
        && mappingProfile != null
        && mappingProfile.getRecordTypes().contains(RecordTypes.HOLDINGS)) {
      defaultRules.addAll(defaultHoldingsRulesFromConfigFile);
    }
    if (mappingProfile == null
        || CollectionUtils.isEmpty(mappingProfile.getTransformations())) {
      log.info("No Mapping rules specified, using default mapping rules");
      return defaultRules;
    }
    List<Rule> rules = new ArrayList<>(
        createByTransformations(mappingProfile.getTransformations(), defaultRules)
    );
    if (isDefaultInstanceProfile(mappingProfile.getId())
        && isNotEmpty(mappingProfile.getTransformations())) {
      rules.addAll(defaultRulesFromConfigFile);
    }
    return rules;
  }

  /**
   * Creates rules by transformations.
   */
  public Set<Rule> createByTransformations(
      List<Transformations> mappingTransformations,
      List<Rule> defaultRules
  ) throws TransformationRuleException {
    Set<Rule> rules = new LinkedHashSet<>();
    String temporaryLocationTransformation =
        getTemporaryLocationTransformation(mappingTransformations);
    Optional<Rule> rule = Optional.empty();
    for (Transformations mappingTransformation : mappingTransformations) {
      if (isTransformationValidAndNotBlank(mappingTransformation)
          && isPermanentLocationNotEqualsTemporaryLocation(
              temporaryLocationTransformation,
              mappingTransformation
          )) {
        rule = ruleBuilders.get(TRANSFORMATION_BUILDER_KEY)
            .build(rules, mappingTransformation);
      } else if (
          isInstanceTransformationValidAndBlank(mappingTransformation)
          || isHoldingsTransformationValidAndBlank(mappingTransformation)
      ) {
        rule = createDefaultByTransformations(mappingTransformation, defaultRules);
      } else if (RecordTypes.ITEM.equals(mappingTransformation.getRecordType())) {
        log.error(
            String.format(
                "No transformation provided for field name: %s, and with record type: %s",
                mappingTransformation.getFieldId(),
                mappingTransformation.getRecordType()
            )
        );
      }
      if (rule.isPresent()) {
        rules.add(rule.get());
      }
    }
    return rules;
  }

  /**
   * Creates default rule by transformation.
   */
  public Optional<Rule> createDefaultByTransformations(
      Transformations mappingTransformation,
      List<Rule> defaultRules
  ) throws TransformationRuleException {
    RecordTypes recordType = mappingTransformation.getRecordType();
    if (TRUE.equals(mappingTransformation.getEnabled())
        && StringUtils.isNotBlank(mappingTransformation.getFieldId())
        && RecordTypes.INSTANCE.equals(recordType)) {
      for (Map.Entry<String, RuleBuilder> ruleBuilderEntry : ruleBuilders.entrySet()) {
        if (mappingTransformation.getFieldId().contains(ruleBuilderEntry.getKey())) {
          return ruleBuilderEntry.getValue().build(defaultRules, mappingTransformation);
        }
      }
      return ruleBuilders.get(DEFAULT_BUILDER_KEY)
          .build(defaultRules, mappingTransformation);
    }
    return Optional.empty();
  }

  /**
   * Checks if the mapping profile is the default instance profile.
   */
  public static boolean isDefaultInstanceProfile(UUID mappingProfileId) {
    return DEFAULT_INSTANCE_MAPPING_PROFILE_ID.equals(mappingProfileId.toString());
  }

  private boolean isTransformationValidAndNotBlank(Transformations mappingTransformation) {
    return isTransformationValid(mappingTransformation)
        && StringUtils.isNotBlank(mappingTransformation.getTransformation());
  }

  private boolean isPermanentLocationNotEqualsTemporaryLocation(
      String temporaryLocationTransformation,
      Transformations mappingTransformation
  ) {
    return !(isHoldingsPermanentLocation(mappingTransformation)
        && temporaryLocationTransformation.equals(mappingTransformation.getTransformation()));
  }

  private boolean isInstanceTransformationValidAndBlank(Transformations mappingTransformation) {
    return isTransformationValid(mappingTransformation)
        && RecordTypes.INSTANCE.equals(mappingTransformation.getRecordType())
        && StringUtils.isBlank(mappingTransformation.getTransformation());
  }

  private boolean isHoldingsTransformationValidAndBlank(Transformations mappingTransformation) {
    return isTransformationValid(mappingTransformation)
        && RecordTypes.HOLDINGS.equals(mappingTransformation.getRecordType())
        && StringUtils.isBlank(mappingTransformation.getTransformation());
  }

  private String getTemporaryLocationTransformation(
      List<Transformations> mappingTransformations
  ) {
    Optional<Transformations> temporaryLocationTransformation = mappingTransformations.stream()
        .filter(transformations ->
            RecordTypes.HOLDINGS.equals(transformations.getRecordType())
        )
        .filter(transformations ->
            TEMPORARY_LOCATION_FIELD_ID.equals(transformations.getFieldId())
        )
        .findFirst();
    if (temporaryLocationTransformation.isPresent()) {
      return temporaryLocationTransformation.get().getTransformation();
    }
    return StringUtils.EMPTY;
  }

  private boolean isHoldingsPermanentLocation(Transformations mappingTransformation) {
    return RecordTypes.HOLDINGS.equals(mappingTransformation.getRecordType())
        && PERMANENT_LOCATION_FIELD_ID.equals(mappingTransformation.getFieldId());
  }

  private boolean isTransformationValid(Transformations mappingTransformation) {
    return Boolean.TRUE.equals(mappingTransformation.getEnabled())
        && StringUtils.isNotBlank(mappingTransformation.getPath());
  }

  private List<Rule> createRulesDependsOnRecordType(
      MappingProfile mappingProfile
  ) throws TransformationRuleException {
    List<Rule> combinedDefaultRules = new ArrayList<>();
    if (mappingProfile == null
        || mappingProfile.getRecordTypes().contains(RecordTypes.INSTANCE)) {
      combinedDefaultRules.addAll(defaultRulesFromConfigFile);
    }
    if (mappingProfile != null
        && (mappingProfile.getRecordTypes().contains(RecordTypes.HOLDINGS))) {
      combinedDefaultRules.addAll(defaultHoldingsRulesFromConfigFile);
    }
    return create(mappingProfile, combinedDefaultRules, false);
  }

}
