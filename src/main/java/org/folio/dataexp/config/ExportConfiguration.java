package org.folio.dataexp.config;

import org.folio.processor.RuleProcessor;
import org.folio.processor.translations.TranslationsFunctionHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExportConfiguration {

  @Bean
  public RuleProcessor ruleProcessor() {
    return new RuleProcessor(TranslationsFunctionHolder.SET_VALUE);
  }
}
