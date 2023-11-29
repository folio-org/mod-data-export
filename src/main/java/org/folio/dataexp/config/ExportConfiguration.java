package org.folio.dataexp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import lombok.extern.log4j.Log4j2;
import org.folio.processor.RuleProcessor;
import org.folio.processor.rule.Rule;
import org.folio.processor.translations.TranslationsFunctionHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Configuration
@Log4j2
public class ExportConfiguration {

  @Bean
  public RuleProcessor ruleProcessor() {
    return new RuleProcessor(TranslationsFunctionHolder.SET_VALUE);
  }

  @Bean
  public List<Rule> defaultRules() throws IOException {
    var mapper = new ObjectMapper();
    try (InputStream is = ExportConfiguration.class.getResourceAsStream("/rules/rulesDefault.json")) {
      List<Rule> defaultRules = mapper.readValue(is, mapper.getTypeFactory().constructCollectionType(List.class, Rule.class));
      return ImmutableList.copyOf(defaultRules);
    } catch (IOException e) {
      log.error("Failed to fetch default rules for export");
      throw e;
    }
  }

  @Bean
  public List<Rule> holdingsDefaultRules() throws IOException {
    var mapper = new ObjectMapper();
    try (InputStream is = ExportConfiguration.class.getResourceAsStream("/rules/holdingsRulesDefault.json")) {
      List<Rule> defaultRules = mapper.readValue(is, mapper.getTypeFactory().constructCollectionType(List.class, Rule.class));
      return ImmutableList.copyOf(defaultRules);
    } catch (IOException e) {
      log.error("Failed to fetch default holdings rules for export");
      throw e;
    }
  }
}
