package org.folio.dataexp.service.export.strategies.handlers;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.folio.processor.rule.DataSource;
import org.folio.processor.rule.Rule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The class to perform pre/post handling logic for mapping rules
 *
 */
@Component
public class RuleHandler {
  private static final String HOLDINGS_KEY = "holdings";

  /* Private constructor to hide the implicit public one */
  private RuleHandler() {
  }

  /**
   * The method adds new rules to the incoming original rules.
   * This method turns rules for items from the starred version "$.holdings[*]..."
   * to the indexed version "$.holdings[0]..." , "$.holdings[1]...", ...
   * and adds data source with sub-field '3' for each of newly created indexed rules
   *
   * @param instance      json object contains instances, holdings and items
   * @param originalRules original mapping rules
   * @return final rules
   */
  public List<Rule> preHandle(JSONObject instance, List<Rule> originalRules) {
    if (!instance.containsKey(HOLDINGS_KEY)) {
      return Collections.synchronizedList(originalRules);
    }
    int numberOfHoldings = ((JSONArray)instance.get(HOLDINGS_KEY)).size();
    List<Rule> starredRules = new ArrayList<>();
    List<Rule> indexedRules = new ArrayList<>();
    for (Rule originRule : originalRules) {
      if (originRule.isItemTypeRule()) {
        for (int holdingIndex = 0; holdingIndex < numberOfHoldings; holdingIndex++) {
          Rule indexedRule = createIndexedRule(originRule, holdingIndex);
          indexedRules.add(indexedRule);
        }
      } else {
        starredRules.add(originRule);
      }
    }
    starredRules.addAll(indexedRules);
    return Collections.synchronizedList(starredRules);
  }

  /**
   * Creates the new indexed rule from the given starred rule, adds data source for the holding hrid
   *
   * @param starredRule  original starred rule
   * @param holdingIndex the index of holding needed to index the rule
   * @return indexed rule
   */
  private Rule createIndexedRule(Rule starredRule, int holdingIndex) {
    Rule indexedRule = starredRule.copy();
    for (DataSource dataSource : indexedRule.getDataSources()) {
      if (dataSource.getFrom() != null) {
        dataSource.setFrom(dataSource.getFrom().replace("$.holdings[*]", "$.holdings[" + holdingIndex + "]"));
      }
    }
    DataSource holdingHridDataSource = new DataSource();
    holdingHridDataSource.setFrom("$.holdings[" + holdingIndex + "].hrid");
    holdingHridDataSource.setSubfield("3");
    indexedRule.getDataSources().add(holdingHridDataSource);
    return indexedRule;
  }
}
