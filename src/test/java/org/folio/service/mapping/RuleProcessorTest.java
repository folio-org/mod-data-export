package org.folio.service.mapping;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.service.mapping.processor.RuleProcessor;
import org.folio.service.mapping.processor.translations.Settings;
import org.folio.service.mapping.reader.EntityReader;
import org.folio.service.mapping.reader.JPathSyntaxEntityReader;
import org.folio.service.mapping.writer.RecordWriter;
import org.folio.service.mapping.writer.impl.JsonRecordWriter;
import org.folio.service.mapping.writer.impl.MarcRecordWriter;
import org.folio.service.mapping.writer.impl.XmlRecordWriter;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

import static org.folio.TestUtil.getResourceAsString;

@RunWith(JUnit4.class)
public class RuleProcessorTest {
  private static JsonObject entity;
  private static JsonArray rules;
  private Settings settings = null;

  @BeforeClass
  public static void setup() throws IOException {
    entity = new JsonObject(getResourceAsString("processor/given_entity.json"));
    rules = new JsonArray(getResourceAsString("processor/test_rules.json"));
  }

  @Test
  public void shouldMapEntityTo_MarcRecord() {
    // given
    RuleProcessor ruleProcessor = new RuleProcessor(rules);
    EntityReader reader = new JPathSyntaxEntityReader(entity);
    RecordWriter writer = new MarcRecordWriter();
    // when
    String actualMarcRecord = ruleProcessor.process(reader, writer, settings);
    // then
    String expectedMarcRecord = getResourceAsString("processor/mapped_marc_record.mrc");
    Assert.assertEquals(expectedMarcRecord, actualMarcRecord);
  }

  @Test
  public void shouldMapEntityTo_JsonRecord() {
    // given
    RuleProcessor ruleProcessor = new RuleProcessor(rules);
    EntityReader reader = new JPathSyntaxEntityReader(entity);
    RecordWriter writer = new JsonRecordWriter();
    // when
    String actualJsonRecord = ruleProcessor.process(reader, writer, settings);
    // then
    String expectedJsonRecord = getResourceAsString("processor/mapped_json_record.json");
    Assert.assertEquals(expectedJsonRecord, actualJsonRecord);
  }

  @Test
  public void shouldMapEntityTo_XmlRecord() {
    // given
    RuleProcessor ruleProcessor = new RuleProcessor(rules);
    EntityReader reader = new JPathSyntaxEntityReader(entity);
    RecordWriter writer = new XmlRecordWriter();
    // when
    String actualXmlRecord = ruleProcessor.process(reader, writer, settings);
    // then
    String expectedXmlRecord = getResourceAsString("processor/mapped_xml_record.xml");
    Assert.assertEquals(expectedXmlRecord, actualXmlRecord);
  }
}
