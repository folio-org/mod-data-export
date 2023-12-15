package org.folio.dataexp.service.export.strategies.handlers;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.folio.processor.rule.DataSource;
import org.folio.processor.rule.Rule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.folio.dataexp.service.export.Constants.HOLDINGS_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RuleHandlerTest {

  @Test
  void preHandleTest() {
    var jsonObject  = new JSONObject();
    var dataSource = new DataSource();
    var ruleHandler = new RuleHandler();

    var rule = new Rule();
    rule.setItemTypeRule(true);
    rule.setField("fieldId");
    rule.setId("ruleId");
    rule.setDataSources(List.of(dataSource));
    var rules = List.of(rule);
    var result = ruleHandler.preHandle(jsonObject, List.of(rule));
    assertEquals(rules, result);

    var holdingJsonObject = new JSONObject();
    var array = new JSONArray();
    array.add(holdingJsonObject);
    jsonObject.put(HOLDINGS_KEY, array);
    result = ruleHandler.preHandle(jsonObject, List.of(rule));

    assertFalse(result.isEmpty());
    var actualRule = result.get(0);
    var dataSources = actualRule.getDataSources();
    var createdDataSource = dataSources.get(1);
    assertEquals("$.holdings[0].hrid", createdDataSource.getFrom());
    assertEquals("3", createdDataSource.getSubfield());
  }
}
