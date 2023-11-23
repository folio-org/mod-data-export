package org.folio.dataexp.service.export.strategies;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.folio.processor.rule.Rule;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Log4j2
@Component
@Getter
public class HoldingsRulesProvider {

  private static final String DEFAULT_HOLDINGS_RULES_PATH = "/rules/holdingsRulesDefault.json";

  private List<Rule> defaultRules;

  @PostConstruct
  private void setDefaultRules() {
    var mapper = new ObjectMapper();
    try (InputStream is = HoldingsRulesProvider.class.getResourceAsStream(DEFAULT_HOLDINGS_RULES_PATH)) {
      this.defaultRules = mapper.readValue(is, mapper.getTypeFactory().constructCollectionType(List.class, Rule.class));
    } catch (IOException e) {
      log.error("Failed to fetch default holdings rules for export");
    }
  }
}
