package org.folio.dataexp.service.export.strategies.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.folio.processor.rule.DataSource;
import org.folio.processor.rule.Rule;
import org.springframework.stereotype.Component;

/**
 * Handles pre/post processing logic for mapping rules in the data export process.
 *
 * <p>This class provides methods to transform item rules from starred to indexed versions,
 * and to add data sources for each indexed rule.
 * </p>
 */
@Component
public class RuleHandler {
  private static final String HOLDINGS_KEY = "holdings";

  /**
   * Adds new rules to the incoming original rules.
   *
   * <p>This method turns rules for items from the starred version {@code "$.holdings[*]..."}
   * to the indexed version {@code "$.holdings[0]..."}, {@code "$.holdings[1]..."}, etc.,
   * and adds a data source with sub-field '3' for each newly created indexed rule.
   * </p>
   *
   * @param instance      JSON object containing instances, holdings, and items
   * @param originalRules original mapping rules
   * @return final list of rules with indexed holdings
   */
  public List<Rule> preHandle(JSONObject instance, List<Rule> originalRules) {
    if (!instance.containsKey(HOLDINGS_KEY)) {
      return Collections.synchronizedList(originalRules);
    }
    int numberOfHoldings = ((JSONArray) instance.get(HOLDINGS_KEY)).size();
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
   * Creates a new indexed rule from the given starred rule and adds a data source for the
   * holding HRID.
   *
   * @param starredRule  original starred rule
   * @param holdingIndex index of the holding to index the rule
   * @return indexed rule with updated data sources
   */
  private Rule createIndexedRule(Rule starredRule, int holdingIndex) {
    Rule indexedRule = starredRule.copy();
    for (DataSource dataSource : indexedRule.getDataSources()) {
      if (dataSource.getFrom() != null) {
        dataSource.setFrom(dataSource.getFrom().replace("$.holdings[*]",
            "$.holdings[" + holdingIndex + "]"));
      }
    }
    DataSource holdingHridDataSource = new DataSource();
    holdingHridDataSource.setFrom("$.holdings[" + holdingIndex + "].hrid");
    holdingHridDataSource.setSubfield("3");
    indexedRule.getDataSources().add(holdingHridDataSource);
    return indexedRule;
  }
}
