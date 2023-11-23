package org.folio.dataexp.service.export.strategies;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.folio.processor.rule.Rule;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Component
public class HoldingsRulesProvider {

  private List<Rule> defaultRules;

  public List<Rule> getDefaultRules() {
    if (this.defaultRules != null) return this.defaultRules;
    var mapper = new ObjectMapper();
    try (InputStream is = HoldingsRulesProvider.class.getResourceAsStream("/rules/holdingsRulesDefault.json")) {
      this.defaultRules = mapper.readValue(is, mapper.getTypeFactory().constructCollectionType(List.class, Rule.class));
      return this.defaultRules;
    } catch (IOException e) {
      log.error("Failed to fetch default holdings rules for export");
    }
    return new ArrayList<>();
  }
}
