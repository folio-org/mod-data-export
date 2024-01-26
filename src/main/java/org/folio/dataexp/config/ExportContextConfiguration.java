package org.folio.dataexp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicBoolean;

@Configuration
public class ExportContextConfiguration {

  @Bean
  public AtomicBoolean lastSlice() {
    return new AtomicBoolean();
  }

  @Bean
  public AtomicBoolean lastExport() {
    return new AtomicBoolean();
  }
}
