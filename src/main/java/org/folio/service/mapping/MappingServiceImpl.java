package org.folio.service.mapping;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.clients.ConfigurationsClient;
import org.folio.rest.jaxrs.model.MappingProfile;
import org.folio.service.mapping.processor.RuleFactory;
import org.folio.service.mapping.processor.RuleProcessor;
import org.folio.service.mapping.processor.rule.Rule;
import org.folio.service.mapping.reader.EntityReader;
import org.folio.service.mapping.reader.JPathSyntaxEntityReader;
import org.folio.service.mapping.referencedata.ReferenceData;
import org.folio.service.mapping.referencedata.ReferenceDataProvider;
import org.folio.service.mapping.writer.RecordWriter;
import org.folio.service.mapping.writer.impl.MarcRecordWriter;
import org.folio.util.OkapiConnectionParams;
import org.marc4j.marc.VariableField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

  public MappingServiceImpl() {
    this.ruleProcessor = new RuleProcessor();
    this.ruleFactory = new RuleFactory();
    this.mappingThreadPool = Executors.newWorkStealingPool(MAPPING_POOL_SIZE);
  }

  @Override
  public List<String> map(List<JsonObject> instances, MappingProfile mappingProfile, String jobExecutionId, OkapiConnectionParams connectionParams) {
    if (CollectionUtils.isEmpty(instances)) {
      return Collections.emptyList();
    }
    ReferenceData referenceData = referenceDataProvider.get(jobExecutionId, connectionParams);
    List<Rule> rules = getRules(mappingProfile, connectionParams);
    return mapInstances(instances, referenceData, rules);
  }

  private List<String> mapInstances(List<JsonObject> instances, ReferenceData referenceData, List<Rule> rules) {
    List<String> records = null;
    try {
      records = mappingThreadPool.submit(() -> instances.parallelStream()
        .map(instance -> mapInstance(instance, referenceData, rules))
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

  private Optional<String> mapInstance(JsonObject instance, ReferenceData referenceData, List<Rule> rules) {
    try {
      EntityReader entityReader = new JPathSyntaxEntityReader(instance);
      RecordWriter recordWriter = new MarcRecordWriter();
      String record = ruleProcessor.process(entityReader, recordWriter, referenceData, rules);
      return Optional.of(record);
    } catch (Exception e) {
      LOGGER.error("Exception occurred while mapping, exception: {}, inventory instance: {}", e, instance);
      return Optional.empty();
    }
  }

  /**
   * This method specifically returns additional records mapped to variable Field format that can be
   * later appended to SRS records.
   */
  @Override
  public List<VariableField> mapFields(JsonObject record, MappingProfile mappingProfile, String jobExecutionId, OkapiConnectionParams connectionParams) {
    ReferenceData referenceData = referenceDataProvider.get(jobExecutionId, connectionParams);
    List<Rule> rules = getRules(mappingProfile, connectionParams);
    EntityReader entityReader = new JPathSyntaxEntityReader(record);
    RecordWriter recordWriter = new MarcRecordWriter();
    return this.ruleProcessor.processFields(entityReader, recordWriter, referenceData, rules);
  }

  private List<Rule> getRules(MappingProfile mappingProfile, OkapiConnectionParams params) {
    List<Rule> rulesFromConfig = configurationsClient.getRulesFromConfiguration(params);
    return CollectionUtils.isEmpty(rulesFromConfig) ? ruleFactory.create(mappingProfile) : appendRulesFromProfile(rulesFromConfig, mappingProfile);
  }

  private List<Rule> appendRulesFromProfile(List<Rule> rulesFromConfig, MappingProfile mappingProfile) {
    if (mappingProfile != null && isNotEmpty(mappingProfile.getTransformations())) {
      LOGGER.debug("Using overridden rules from mod-configuration with transformations from the mapping profile with id {}", mappingProfile.getId());
      rulesFromConfig.addAll(ruleFactory.buildByTransformations(mappingProfile.getTransformations()));
    }
    return rulesFromConfig;
  }

}
