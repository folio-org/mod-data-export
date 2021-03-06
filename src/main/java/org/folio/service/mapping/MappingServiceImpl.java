package org.folio.service.mapping;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.util.ErrorCode.ERROR_FIELDS_MAPPING_INVENTORY;
import static org.folio.util.ErrorCode.ERROR_FIELDS_MAPPING_INVENTORY_WITH_REASON;
import static org.folio.util.ErrorCode.ERROR_FIELDS_MAPPING_SRS;

import io.vertx.core.json.JsonObject;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.clients.ConfigurationsClient;
import org.folio.processor.RuleProcessor;
import org.folio.processor.referencedata.JsonObjectWrapper;
import org.folio.processor.referencedata.ReferenceDataWrapper;
import org.folio.processor.referencedata.ReferenceDataWrapperImpl;
import org.folio.processor.rule.Rule;
import org.folio.processor.translations.TranslationsFunctionHolder;
import org.folio.reader.EntityReader;
import org.folio.reader.JPathSyntaxEntityReader;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.service.logs.ErrorLogService;
import org.folio.service.mapping.handler.RuleHandler;
import org.folio.service.mapping.referencedata.ReferenceData;
import org.folio.service.mapping.referencedata.ReferenceDataProvider;
import org.folio.util.OkapiConnectionParams;
import org.folio.writer.RecordWriter;
import org.folio.writer.impl.MarcRecordWriter;
import org.marc4j.marc.VariableField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MappingServiceImpl implements MappingService {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());
  private static final int MAPPING_POOL_SIZE = 4;

  private ExecutorService mappingThreadPool;
  private final RuleFactory ruleFactory;
  private final RuleProcessor ruleProcessor;

  @Autowired
  private ReferenceDataProvider referenceDataProvider;
  @Autowired
  private ConfigurationsClient configurationsClient;
  @Autowired
  private ErrorLogService errorLogService;

  public MappingServiceImpl() {
    this.ruleProcessor = new RuleProcessor(TranslationsFunctionHolder.SET_VALUE);
    this.ruleFactory = new RuleFactory();
    this.mappingThreadPool = Executors.newWorkStealingPool(MAPPING_POOL_SIZE);
  }

  @Override
  public Pair<List<String>, Integer> map(List<JsonObject> instances, MappingProfile mappingProfile, String jobExecutionId, OkapiConnectionParams connectionParams) {
    if (CollectionUtils.isEmpty(instances)) {
      return Pair.of(Collections.emptyList(), 0);
    }
    ReferenceData referenceData = referenceDataProvider.get(jobExecutionId, connectionParams);
    List<Rule> rules = getRules(mappingProfile, jobExecutionId, connectionParams);
    return mapInstances(instances, referenceData, rules, jobExecutionId, connectionParams);
  }

  private Pair<List<String>, Integer> mapInstances(List<JsonObject> instances, ReferenceData referenceData,
    List<Rule> rules, String jobExecutionId, OkapiConnectionParams connectionParams) {
    List<Rule> synchronizedRules = Collections.synchronizedList(rules);
    List<String> records = null;
    int failedCount = 0;
    try {
      List<Pair<Optional<String>, Integer>> list = mappingThreadPool.submit(() -> instances.parallelStream()
        .map(instance -> mapInstance(instance, referenceData, synchronizedRules, jobExecutionId, connectionParams))
        .collect(Collectors.toList()))
        .get();
      failedCount = list.stream().mapToInt(Pair::getValue).sum();
      records = list.stream()
        .filter(integerPair -> integerPair.getKey().isPresent())
        .map(result -> result.getKey().get())
        .collect(Collectors.toList());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.warn("Interrupting the current thread {}", Thread.currentThread().getName());
    } catch (ExecutionException e) {
      LOGGER.error("Exception occurred while run mapping {}", e.getMessage());
    }
    return Pair.of(records, failedCount);
  }

  private Pair<Optional<String>, Integer> mapInstance(JsonObject instance, ReferenceData referenceData, List<Rule> originalRules, String jobExecutionId, OkapiConnectionParams connectionParams) {
    try {
      List<Rule> finalRules = RuleHandler.preHandle(instance, originalRules);
      return mapInstance(instance, referenceData, jobExecutionId, finalRules, connectionParams);
    } catch (Exception e) {
      LOGGER.debug("Exception occurred while mapping, exception: {}, inventory instance: {}", e, instance);
      errorLogService.saveGeneralError(ERROR_FIELDS_MAPPING_INVENTORY.getCode(), jobExecutionId, connectionParams.getTenantId());
      return Pair.of(Optional.empty(), 0);
    }
  }

  protected Pair<Optional<String>, Integer> mapInstance(JsonObject instance, ReferenceData referenceData, String jobExecutionId,  List<Rule> rules, OkapiConnectionParams connectionParams) {
    EntityReader entityReader = new JPathSyntaxEntityReader(instance.encode());
    RecordWriter recordWriter = new MarcRecordWriter();
    Set<String> failedCount = new HashSet<>();
    ReferenceDataWrapper referenceDataWrapper = getReferenceDataWrapper(referenceData);
    String record = ruleProcessor.process(entityReader, recordWriter, referenceDataWrapper, rules, (translationException -> {
      LOGGER.debug("Exception occurred while mapping, exception: {}, inventory instance: {}", translationException.getCause(), instance);
      errorLogService.saveWithAffectedRecord(instance, ERROR_FIELDS_MAPPING_INVENTORY_WITH_REASON.getCode(), jobExecutionId, translationException, connectionParams);
      failedCount.add(translationException.getRecordInfo().getId());
    }));
    return Pair.of(Optional.of(record), failedCount.size());
  }

  /**
   * This method specifically returns additional records mapped to variable Field format that can be
   * later appended to SRS records.
   */
  @Override
  public Pair<List<VariableField>, Integer> mapFields(JsonObject record, MappingProfile mappingProfile,
    String jobExecutionId, OkapiConnectionParams connectionParams) {
    ReferenceData referenceData = referenceDataProvider.get(jobExecutionId, connectionParams);
    List<Rule> rules = getRules(mappingProfile, jobExecutionId, connectionParams);
    List<Rule> finalRules = RuleHandler.preHandle(record, rules);
    Set<String> failedCount = new HashSet<>();
    EntityReader entityReader = new JPathSyntaxEntityReader(record.encode());
    RecordWriter recordWriter = new MarcRecordWriter();
    ReferenceDataWrapper referenceDataWrapper = getReferenceDataWrapper(referenceData);
    List<VariableField> mappedRecord = ruleProcessor
      .processFields(entityReader, recordWriter, referenceDataWrapper, finalRules, (translationException -> {
        List<String> errorMessageValues = Arrays
          .asList(translationException.getRecordInfo().getId(), translationException.getErrorCode().getDescription(),
            translationException.getMessage());
        errorLogService
          .saveGeneralErrorWithMessageValues(ERROR_FIELDS_MAPPING_SRS.getCode(), errorMessageValues, jobExecutionId,
            connectionParams.getTenantId());
        failedCount.add(translationException.getRecordInfo().getId());
      }));
    return Pair.of(mappedRecord, failedCount.size());
  }

  private ReferenceDataWrapper getReferenceDataWrapper(ReferenceData referenceData) {
    if (referenceData == null) {
      return null;
    }
    Map<String, Map<String, JsonObjectWrapper>> referenceDataWrapper = referenceData.getReferenceData().entrySet().stream()
      .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().entrySet().stream()
          .collect(Collectors.toMap(Entry::getKey, value -> new JsonObjectWrapper(value.getValue().getMap())))));
    return new ReferenceDataWrapperImpl(referenceDataWrapper);
  }

  private List<Rule> getRules(MappingProfile mappingProfile, String jobExecutionId, OkapiConnectionParams params) {
    if (mappingProfile != null && !mappingProfile.getRecordTypes().contains(RecordType.INSTANCE)) {
      return ruleFactory.create(mappingProfile);
    }
    List<Rule> rulesFromConfig = configurationsClient.getRulesFromConfiguration(jobExecutionId, params);
    if (mappingProfile != null && isNotEmpty(rulesFromConfig)) {
      LOGGER.debug("Using overridden rules from mod-configuration with transformations from the mapping profile with id {}", mappingProfile.getId());
    }
    return CollectionUtils.isEmpty(rulesFromConfig) ? ruleFactory.create(mappingProfile) : ruleFactory.create(mappingProfile, rulesFromConfig, true);
  }

}
