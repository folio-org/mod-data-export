package org.folio.service.mapping;

import com.google.common.io.Resources;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.folio.service.mapping.processor.RuleProcessor;
import org.folio.service.mapping.processor.rule.Rules;
import org.folio.service.mapping.processor.translations.Settings;
import org.folio.service.mapping.reader.EntityReader;
import org.folio.service.mapping.reader.JPathSyntaxEntityReader;
import org.folio.service.mapping.writer.RecordWriter;
import org.folio.service.mapping.writer.impl.MarcRecordWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MappingServiceImpl implements MappingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private RuleProcessor ruleProcessor;
  private Settings settings = null;

  public MappingServiceImpl() {
    try {
      URL url = Resources.getResource("rules/rulesDefault.json");
      this.ruleProcessor = new RuleProcessor(Json.decodeValue(Resources.toString(url, StandardCharsets.UTF_8), Rules.class));
    } catch (IOException exception) {
      LOGGER.error("Exception occurred while initializing MappingService", exception);
    }
  }

  @Override
  public List<String> map(List<JsonObject> instances) {
    List<String> records = new ArrayList<>();
    for (JsonObject instance : instances) {
      String record = runDefaultMapping(instance);
      records.add(record);
    }
    return records;
  }

  public String runDefaultMapping(JsonObject instance) {
    EntityReader entityReader = new JPathSyntaxEntityReader(instance);
    RecordWriter recordWriter = new MarcRecordWriter();
    return this.ruleProcessor.process(entityReader, recordWriter, settings);
  }
}
