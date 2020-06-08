package org.folio.service.mapping;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
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
  private final RuleFactory ruleFactory;
  private final RuleProcessor ruleProcessor;
  @Autowired
  private ReferenceDataProvider referenceDataProvider;

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
    List<Rule> rules = ruleFactory.create(mappingProfile);
    for (JsonObject instance : instances) {
      String record = runDefaultMapping(instance, referenceData, rules);
      records.add(record);
    }
    return records;
  }

  private String runDefaultMapping(JsonObject instance, ReferenceData referenceData, List<Rule> rules) {
    EntityReader entityReader = new JPathSyntaxEntityReader(instance);
    RecordWriter recordWriter = new MarcRecordWriter();
    return this.ruleProcessor.process(entityReader, recordWriter, referenceData, rules);
  }

  @Override
  public List<VariableField> mapFields(JsonObject record, MappingProfile mappingProfile, String jobExecutionId, OkapiConnectionParams connectionParams) {
    ReferenceData referenceData = referenceDataProvider.get(jobExecutionId, connectionParams);
    List<Rule> rules = ruleFactory.create(mappingProfile);
    EntityReader entityReader = new JPathSyntaxEntityReader(record);
    RecordWriter recordWriter = new MarcRecordWriter();
    return this.ruleProcessor.processFields(entityReader, recordWriter, referenceData, rules);
  }

}
