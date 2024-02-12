package org.folio.dataexp.repository;

import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.entity.ConfigurationEntity;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigurationRepositoryTest extends BaseDataExportInitializer {

  @Autowired
  private ConfigurationRepository configurationRepository;

  @Test
  void saveConfigurationTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      configurationRepository.deleteAll();

      configurationRepository.save(ConfigurationEntity.builder()
        .key("config1").value("1").build());
      configurationRepository.save(ConfigurationEntity.builder()
        .key("config2").value("2").build());
      configurationRepository.save(ConfigurationEntity.builder()
        .key("config3").value("3").build());
      var expectedAmountOfConfigs = 3;
      var actualAmountOfConfigs = configurationRepository.findAll().size();
      assertEquals(expectedAmountOfConfigs, actualAmountOfConfigs);

      // Update config3.
      configurationRepository.save(ConfigurationEntity.builder()
        .key("config3").value("4").build());
      actualAmountOfConfigs = configurationRepository.findAll().size();

      // Expected amount of configs remains the same: 3.
      assertEquals(expectedAmountOfConfigs, actualAmountOfConfigs);
    }
  }

  @Test
  void getConfigurationTest() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      configurationRepository.save(ConfigurationEntity.builder()
        .key("config1").value("1").build());
      configurationRepository.save(ConfigurationEntity.builder()
        .key("config2").value("2").build());
      var expectedValue = "1";
      var actualValue = configurationRepository.getReferenceById("config1").getValue();
      assertEquals(expectedValue, actualValue);
      expectedValue = "2";
      actualValue = configurationRepository.getReferenceById("config2").getValue();
      assertEquals(expectedValue, actualValue);

      // Update config2.
      configurationRepository.save(ConfigurationEntity.builder()
        .key("config2").value("3").build());
      expectedValue = "3";
      actualValue = configurationRepository.getReferenceById("config2").getValue();
      assertEquals(expectedValue, actualValue);
    }
  }
}
