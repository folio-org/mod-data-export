package org.folio.service.mapping;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.clients.ConfigurationsClient;
import org.folio.processor.ReferenceData;
import org.folio.processor.RuleProcessor;
import org.folio.processor.rule.Rule;
import org.folio.processor.translations.TranslationsFunctionHolder;
import org.folio.reader.EntityReader;
import org.folio.reader.JPathSyntaxEntityReader;
import org.folio.rest.jaxrs.model.MappingProfile;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class MappingServiceImpl implements MappingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
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
      return mapInstance(instance, referenceData, finalRules);
    } catch (Exception e) {
      LOGGER.debug("Exception occurred while mapping, exception: {}, inventory instance: {}", e, instance);
      errorLogService.saveWithAffectedRecord(instance, "An error occurred during fields mapping", jobExecutionId, connectionParams.getTenantId());
      return Optional.empty();
    }
  }

  protected Optional<String> mapInstance(JsonObject instance, ReferenceData referenceData, List<Rule> rules) {
    EntityReader entityReader = new JPathSyntaxEntityReader(instance);
    RecordWriter recordWriter = new MarcRecordWriter();
    String record = ruleProcessor.process(entityReader, recordWriter, referenceData, rules);
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
    return ruleProcessor.processFields(entityReader, recordWriter, referenceData, rules);
  }

  private List<Rule> getRules(MappingProfile mappingProfile, String jobExecutionId, OkapiConnectionParams params) {
    List<Rule> rulesFromConfig = configurationsClient.getRulesFromConfiguration(jobExecutionId, params);
    if (mappingProfile != null && CollectionUtils.isNotEmpty(rulesFromConfig)) {
      LOGGER.debug("Using overridden rules from mod-configuration with transformations from the mapping profile with id {}", mappingProfile.getId());
    }
    return CollectionUtils.isEmpty(rulesFromConfig) ? ruleFactory.create(mappingProfile) : ruleFactory.create(mappingProfile, rulesFromConfig);
  }

}
