package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.Config;
import org.folio.dataexp.domain.entity.ConfigurationEntity;
import org.folio.dataexp.repository.ConfigurationRepository;
import org.folio.dataexp.service.validators.ConfigurationValidator;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Log4j2
@Service
public class ConfigurationService {

  private final ConfigurationRepository configurationRepository;
  private final ConfigurationValidator configurationValidator;

  public Config upsertConfiguration(Config config) {
    log.info("Upserting configuration by id {}", config.getKey());
    configurationValidator.validate(config);
    var entity = ConfigurationEntity.builder()
      .key(config.getKey())
      .value(config.getValue()).build();
    var saved = configurationRepository.save(entity);
    log.info("Upserted successfully: {}", saved.getValue());
    return new Config().key(saved.getKey()).value(saved.getValue());
  }

  public String getValue(String key) {
    return configurationRepository.getReferenceById(key).getValue();
  }

}
