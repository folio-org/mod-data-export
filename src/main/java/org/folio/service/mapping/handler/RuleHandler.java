package org.folio.service.mapping.handler;

import io.vertx.core.json.JsonObject;
import org.folio.processor.rule.DataSource;
import org.folio.processor.rule.Rule;
import org.folio.service.mapping.MappingService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The util class to perform pre/post handling logic for rules
 *
 * @see MappingService
 */
public class RuleHandler {
  private static final String HOLDINGS_KEY = "holdings";

  /* Private constructor to hide the implicit public one */
  private RuleHandler() {
  }

  /**
   * The method adds new rules to the incoming original rules.
   * This method turns rules for holdings from starred version "$.holdings[*]..."
   * to the indexed version "$.holdings[0]..." , "$.holdings[1]...", ...
   * and adds data source with subfield '3' for each of them
   *
   * @param instance      json object contains instances, holdings and items
   * @param originalRules original mapping rules
   * @return final rules
   */
  public static List<Rule> preHandle(JsonObject instance, List<Rule> originalRules) {
    if (!instance.containsKey(HOLDINGS_KEY)) {
      return Collections.synchronizedList(originalRules);
    }
    int numberOfHoldings = instance.getJsonArray(HOLDINGS_KEY).size();
    List<Rule> starredRules = new ArrayList<>();
    List<Rule> indexedRules = new ArrayList<>();
    for (Rule originRule : originalRules) {
      boolean hasRuleSameFieldInHoldings = false;
      for (DataSource targetDataSource : originRule.getDataSources()) {
        if (targetDataSource.isHasSameTagInItems()) {
          hasRuleSameFieldInHoldings = true;
          for (int holdingIndex = 0; holdingIndex < numberOfHoldings; holdingIndex++) {
            Rule indexedRule = originRule.copy();
            indexRule(targetDataSource, holdingIndex, indexedRule);
            addHridDataSource(indexedRules, holdingIndex, indexedRule);
          }
        }
      }
      if (!hasRuleSameFieldInHoldings) {
        starredRules.add(originRule);
      }
    }
    starredRules.addAll(indexedRules);
    return Collections.synchronizedList(starredRules);
  }

  private static void indexRule(DataSource targetDataSource, int holdingIndex, Rule indexedRule) {
    for (DataSource dataSource : indexedRule.getDataSources()) {
      if (dataSource.getFrom() != null && dataSource.getFrom().equals(targetDataSource.getFrom())) {
        dataSource.setFrom(targetDataSource.getFrom().replace("$.holdings[*]", "$.holdings[" + holdingIndex + "]"));
      }
    }
  }

  private static void addHridDataSource(List<Rule> indexedRules, int holdingIndex, Rule indexedRule) {
    DataSource holdingHridDataSource = new DataSource();
    holdingHridDataSource.setFrom("$.holdings[" + holdingIndex + "].hrid");
    holdingHridDataSource.setSubfield("3");
    holdingHridDataSource.setReadDependingOnDataSource(0);
    indexedRule.getDataSources().add(holdingHridDataSource);
    indexedRules.add(indexedRule);
  }
}
