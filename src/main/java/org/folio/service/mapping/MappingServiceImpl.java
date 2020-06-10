package org.folio.service.mapping;

import io.vertx.core.json.JsonObject;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Service
public class MappingServiceImpl implements MappingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final RuleFactory ruleFactory;
  private final RuleProcessor ruleProcessor;
  @Autowired
  private ReferenceDataProvider referenceDataProvider;
  @Autowired
  private ConfigurationsClient configurationsClient;

  public MappingServiceImpl() {
    this.ruleProcessor = new RuleProcessor();
    this.ruleFactory = new RuleFactory();
  }

  @Override
  public List<String> map(List<JsonObject> instances, MappingProfile mappingProfile, String jobExecutionId, OkapiConnectionParams connectionParams) {
    if (CollectionUtils.isEmpty(instances)) {
      return Collections.emptyList();
    }
    List<String> records = new ArrayList<>();
    ReferenceData referenceData = referenceDataProvider.get(jobExecutionId, connectionParams);
    List<Rule> rules = getRules(mappingProfile, connectionParams);
    for (JsonObject instance : instances) {
      String record = runMappingProcess(instance, referenceData, rules);
      records.add(record);
    }
    return records;
  }

  private String runMappingProcess(JsonObject instance, ReferenceData referenceData, List<Rule> rules) {
    EntityReader entityReader = new JPathSyntaxEntityReader(instance);
    RecordWriter recordWriter = new MarcRecordWriter();
    return this.ruleProcessor.process(entityReader, recordWriter, referenceData, rules);
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
