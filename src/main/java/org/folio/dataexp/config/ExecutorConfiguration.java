package org.folio.dataexp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ExecutorConfiguration {

  @Value("#{ T(Integer).parseInt('${application.export-files.max-pool-size}')}")
  private int maxPollSize;

  @Bean
  public TaskExecutor singleExportFileTaskExecutor() {
    var executor = new ThreadPoolTaskExecutor();
    executor.setMaxPoolSize(maxPollSize);
    executor.initialize();
    return executor;
  }
}
