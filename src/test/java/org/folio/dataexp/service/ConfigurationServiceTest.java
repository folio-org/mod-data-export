package org.folio.dataexp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.domain.dto.Config;
import org.folio.dataexp.repository.ConfigurationRepository;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ConfigurationServiceTest extends BaseDataExportInitializer {

  @Autowired
  private ConfigurationService configurationService;

  @Autowired
  private ConfigurationRepository configurationRepository;

  @AfterEach
  void eachTearDown() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      configurationRepository.deleteAll();
    }
  }

  @Test
  void upsertConfigurationTestInsert() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      var config = new Config().key("config1").value("1");
      var inserted = configurationService.upsertConfiguration(config);
      assertEquals(config, inserted);
      var configEntity = configurationRepository.getReferenceById("config1");
      assertEquals(inserted.getKey(), configEntity.getKey());
      assertEquals(inserted.getValue(), configEntity.getValue());
    }
  }

  @Test
  void upsertConfigurationTestUpdate() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      configurationService.upsertConfiguration(new Config().key("config1").value("1"));
      configurationService.upsertConfiguration(new Config().key("config1").value("2"));
      var configEntity = configurationRepository.getReferenceById("config1");
      assertEquals(1, configurationRepository.findAll().size());
      assertEquals("config1", configEntity.getKey());
      assertEquals("2", configEntity.getValue());
    }
  }

  @Test
  void getValueTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      configurationService.upsertConfiguration(new Config().key("config1").value("1"));
      configurationService.upsertConfiguration(new Config().key("config2").value("2"));
      assertEquals("1", configurationService.getValue("config1"));
      assertEquals("2", configurationService.getValue("config2"));
    }
  }
}
