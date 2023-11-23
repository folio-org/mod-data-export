package org.folio.dataexp.service.export.strategies;

import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.processor.rule.Rule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Log4j2
@Component
public class HoldingsRulesProvider {

  @Qualifier("holdingsDefaultRules")
  private List<Rule> defaultRules;

  public List<Rule> getRules(MappingProfile mappingProfile) {
    return defaultRules;
  }
}
