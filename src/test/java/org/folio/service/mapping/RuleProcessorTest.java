package org.folio.service.mapping;

import static org.folio.TestUtil.readFileContentFromResources;
import static org.junit.Assert.assertEquals;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.List;
import org.folio.service.mapping.processor.RuleProcessor;
import org.folio.service.mapping.processor.rule.Rule;
import org.folio.service.mapping.reader.EntityReader;
import org.folio.service.mapping.reader.JPathSyntaxEntityReader;
import org.folio.service.mapping.referencedata.ReferenceData;
import org.folio.service.mapping.writer.RecordWriter;
import org.folio.service.mapping.writer.impl.JsonRecordWriter;
import org.folio.service.mapping.writer.impl.MarcRecordWriter;
import org.folio.service.mapping.writer.impl.XmlRecordWriter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
class RuleProcessorTest {
  private static JsonObject entity;
  private static List<Rule> rules;
  private ReferenceData referenceData = null;

  @BeforeAll
  static void setup() {
    entity = new JsonObject(readFileContentFromResources("processor/given_entity.json"));
    rules = Arrays.asList(Json.decodeValue(readFileContentFromResources("processor/test_rules.json"), Rule[].class));
  }

  @Test
  void shouldMapEntityTo_MarcRecord() {
    // given
    RuleProcessor ruleProcessor = new RuleProcessor();
    EntityReader reader = new JPathSyntaxEntityReader(entity);
    RecordWriter writer = new MarcRecordWriter();
    // when
    String actualMarcRecord = ruleProcessor.process(reader, writer, referenceData, rules);
    // then
    String expectedMarcRecord = readFileContentFromResources("processor/mapped_marc_record.mrc");
    assertEquals(expectedMarcRecord, actualMarcRecord);
  }

  @Test
  void shouldMapEntityTo_JsonRecord() {
    // given
    RuleProcessor ruleProcessor = new RuleProcessor();
    EntityReader reader = new JPathSyntaxEntityReader(entity);
    RecordWriter writer = new JsonRecordWriter();
    // when
    String actualJsonRecord = ruleProcessor.process(reader, writer, referenceData, rules);
    // then
    String expectedJsonRecord = readFileContentFromResources("processor/mapped_json_record.json");
    assertEquals(expectedJsonRecord, actualJsonRecord);
  }

  @Test
  void shouldMapEntityTo_XmlRecord() {
    // given
    RuleProcessor ruleProcessor = new RuleProcessor();
    EntityReader reader = new JPathSyntaxEntityReader(entity);
    RecordWriter writer = new XmlRecordWriter();
    // when
    String actualXmlRecord = ruleProcessor.process(reader, writer, referenceData, rules);
    // then
    String expectedXmlRecord = readFileContentFromResources("processor/mapped_xml_record.xml");
    assertEquals(expectedXmlRecord, actualXmlRecord);
  }
}
