package org.folio.service.mapping;

import io.vertx.core.json.JsonObject;
import org.folio.service.mapping.processor.RuleProcessor;
import org.folio.service.mapping.processor.rule.Rule;
import org.folio.service.mapping.reader.EntityReader;
import org.folio.service.mapping.reader.JPathSyntaxEntityReader;
import org.folio.service.mapping.settings.MappingSettingsProvider;
import org.folio.service.mapping.settings.Settings;
import org.folio.service.mapping.writer.RecordWriter;
import org.folio.service.mapping.writer.impl.MarcRecordWriter;
import org.folio.util.OkapiConnectionParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

@Service
public class MappingServiceImpl implements MappingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private RuleProcessor ruleProcessor;
  @Autowired
  private MappingSettingsProvider settingsProvider;


  @Override
  public List<String> map(List<JsonObject> instances, String jobExecutionId, OkapiConnectionParams connectionParams, List<Rule> rules) {
    List<String> records = new ArrayList<>();
    Settings settings = settingsProvider.getSettings(jobExecutionId, connectionParams);
    for (JsonObject instance : instances) {
      String record = runDefaultMapping(instance, settings, rules);
      records.add(record);
    }
    return records;
  }

  private String runDefaultMapping(JsonObject instance, Settings settings, List<Rule> rules) {
    EntityReader entityReader = new JPathSyntaxEntityReader(instance);
    RecordWriter recordWriter = new MarcRecordWriter();
    return this.ruleProcessor.process(entityReader, recordWriter, settings, rules);
  }

  public RuleProcessor getRuleProcessor() {
    return ruleProcessor;
  }

  public void setRuleProcessor(RuleProcessor ruleProcessor) {
    this.ruleProcessor = ruleProcessor;
  }
}
