package org.folio.dataexp.config;

import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Configuration class for setting up the TaskExecutor for export files. */
@Configuration
public class ExecutorConfiguration {

  @Value("#{ T(Integer).parseInt('${application.export-files.max-pool-size}')}")
  private int maxPollSize;

  /**
   * Creates a TaskExecutor with a fixed thread pool size for export file processing.
   *
   * @return a configured TaskExecutor
   */
  @Bean
  public TaskExecutor singleExportFileTaskExecutor() {
    var executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(maxPollSize);
    executor.setMaxPoolSize(maxPollSize);
    executor.setTaskDecorator(
        FolioExecutionScopeExecutionContextManager::getRunnableWithCurrentFolioContext);
    executor.initialize();
    return executor;
  }
}
