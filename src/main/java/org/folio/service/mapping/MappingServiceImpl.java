package org.folio.service.mapping;

import com.google.common.io.Resources;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.folio.service.mapping.processor.RuleProcessor;
import org.folio.service.mapping.processor.rule.Rule;
import org.folio.service.mapping.settings.Settings;
import org.folio.service.mapping.reader.EntityReader;
import org.folio.service.mapping.reader.JPathSyntaxEntityReader;
import org.folio.service.mapping.settings.MappingSettingsProvider;
import org.folio.service.mapping.writer.RecordWriter;
import org.folio.service.mapping.writer.impl.MarcRecordWriter;
import org.folio.util.OkapiConnectionParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class MappingServiceImpl implements MappingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private RuleProcessor ruleProcessor;
  private MappingSettingsProvider settingsProvider;

  public MappingServiceImpl(@Autowired MappingSettingsProvider mappingSettingsProvider) {
    try {
      URL url = Resources.getResource("rules/rulesDefault.json");
      String stringRules = Resources.toString(url, StandardCharsets.UTF_8);
      List<Rule> rules = Arrays.asList(Json.decodeValue(stringRules, Rule[].class));
      this.ruleProcessor = new RuleProcessor(rules);
      this.settingsProvider = mappingSettingsProvider;
    } catch (IOException exception) {
      LOGGER.error("Exception occurred while initializing MappingService", exception);
    }
  }

  @Override
  public List<String> map(List<JsonObject> instances, String jobExecutionId, OkapiConnectionParams connectionParams) {
    List<String> records = new ArrayList<>();
    Settings settings = settingsProvider.getSettings(jobExecutionId, connectionParams);
    for (JsonObject instance : instances) {
      String record = runDefaultMapping(instance, settings);
      records.add(record);
    }
    return records;
  }

  private String runDefaultMapping(JsonObject instance, Settings settings) {
    EntityReader entityReader = new JPathSyntaxEntityReader(instance);
    RecordWriter recordWriter = new MarcRecordWriter();
    return this.ruleProcessor.process(entityReader, recordWriter, settings);
  }
}
