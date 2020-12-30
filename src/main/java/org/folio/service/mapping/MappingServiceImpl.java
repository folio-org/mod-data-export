package org.folio.service.mapping;

import static org.folio.util.ErrorCode.ERROR_FIELDS_MAPPING_INVENTORY;
import static org.folio.util.ErrorCode.ERROR_FIELDS_MAPPING_INVENTORY_WITH_REASON;
import static org.folio.util.ErrorCode.ERROR_FIELDS_MAPPING_SRS;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.clients.ConfigurationsClient;
import org.folio.processor.RuleProcessor;
import org.folio.processor.referencedata.ReferenceData;
import org.folio.processor.rule.Rule;
import org.folio.processor.translations.TranslationsFunctionHolder;
import org.folio.reader.EntityReader;
import org.folio.reader.JPathSyntaxEntityReader;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.rest.jaxrs.model.RecordType;
import org.folio.service.logs.ErrorLogService;
import org.folio.service.mapping.handler.RuleHandler;
import org.folio.service.mapping.referencedata.ReferenceDataProvider;
import org.folio.util.OkapiConnectionParams;
import org.folio.writer.RecordWriter;
import org.folio.writer.impl.MarcRecordWriter;
import org.marc4j.marc.VariableField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

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
  public List<String> map(List<JsonObject> instances, MappingProfile mappingProfile, String jobExecutionId, OkapiConnectionParams connectionParams) {
    if (CollectionUtils.isEmpty(instances)) {
      return Collections.emptyList();
    }
    ReferenceData referenceData = referenceDataProvider.get(jobExecutionId, connectionParams);
    List<Rule> rules = getRules(mappingProfile, jobExecutionId, connectionParams);
    return mapInstances(instances, referenceData, rules, jobExecutionId, connectionParams);
  }

  private List<String> mapInstances(List<JsonObject> instances, ReferenceData referenceData, List<Rule> rules, String jobExecutionId, OkapiConnectionParams connectionParams) {
    List<Rule> synchronizedRules = Collections.synchronizedList(rules);
    List<String> records = null;
    try {
      records = mappingThreadPool.submit(() -> instances.parallelStream()
        .map(instance -> mapInstance(instance, referenceData, synchronizedRules, jobExecutionId, connectionParams))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList()))
        .get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.warn("Interrupting the current thread {}", Thread.currentThread().getName());
    } catch (ExecutionException e) {
      LOGGER.error("Exception occurred while run mapping {}", e);
    }
    return records;
  }

  private Optional<String> mapInstance(JsonObject instance, ReferenceData referenceData, List<Rule> originalRules, String jobExecutionId, OkapiConnectionParams connectionParams) {
    try {
      List<Rule> finalRules = RuleHandler.preHandle(instance, originalRules);
      return mapInstance(instance, referenceData, jobExecutionId, finalRules, connectionParams);
    } catch (Exception e) {
      LOGGER.debug("Exception occurred while mapping, exception: {}, inventory instance: {}", e, instance);
      errorLogService.saveGeneralError(ERROR_FIELDS_MAPPING_INVENTORY.getCode(), jobExecutionId, connectionParams.getTenantId());
      return Optional.empty();
    }
  }

  protected Optional<String> mapInstance(JsonObject instance, ReferenceData referenceData, String jobExecutionId,  List<Rule> rules, OkapiConnectionParams connectionParams) {
    EntityReader entityReader = new JPathSyntaxEntityReader(instance);
    RecordWriter recordWriter = new MarcRecordWriter();
    String record = ruleProcessor.process(entityReader, recordWriter, referenceData, rules, (translationException -> {
      LOGGER.debug("Exception occurred while mapping, exception: {}, inventory instance: {}", translationException.getCause(), instance);
      List<String> errorMessageValues = Arrays.asList(translationException.getErrorCode().getDescription(), translationException.getMessage());
      errorLogService.saveWithAffectedRecord(instance, ERROR_FIELDS_MAPPING_INVENTORY_WITH_REASON.getCode(), errorMessageValues, jobExecutionId, translationException, connectionParams);
    }));
    return Optional.of(record);
  }

  /**
   * This method specifically returns additional records mapped to variable Field format that can be
   * later appended to SRS records.
   */
  @Override
  public List<VariableField> mapFields(JsonObject record, MappingProfile mappingProfile, String jobExecutionId, OkapiConnectionParams connectionParams) {
    ReferenceData referenceData = referenceDataProvider.get(jobExecutionId, connectionParams);
    List<Rule> rules = getRules(mappingProfile, jobExecutionId, connectionParams);
    EntityReader entityReader = new JPathSyntaxEntityReader(record);
    RecordWriter recordWriter = new MarcRecordWriter();
    return ruleProcessor.processFields(entityReader, recordWriter, referenceData, rules, (translationException -> {
      List<String> errorMessageValues = Arrays.asList(translationException.getRecordInfo().getId(), translationException.getErrorCode().getDescription(), translationException.getMessage());
      errorLogService.saveGeneralErrorWithMessageValues(ERROR_FIELDS_MAPPING_SRS.getCode(), errorMessageValues, jobExecutionId, connectionParams.getTenantId());
    }));
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
