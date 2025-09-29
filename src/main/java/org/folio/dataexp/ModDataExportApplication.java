package org.folio.dataexp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the mod-data-export Spring Boot application.
 */
@SpringBootApplication
@EnableFeignClients
@EnableAsync
public class ModDataExportApplication {

  /**
   * Starts the mod-data-export Spring Boot application.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(ModDataExportApplication.class, args);
  }
}
