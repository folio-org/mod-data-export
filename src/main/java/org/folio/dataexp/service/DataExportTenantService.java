package org.folio.dataexp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dataexp.domain.dto.Config;
import org.folio.dataexp.domain.dto.JobProfile;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for tenant initialization and reference/configuration data loading.
 */
@Service
@Primary
@Log4j2
public class DataExportTenantService extends TenantService {

  private static final List<String> MAPPING_PROFILES =
      List.of(
          "default_authority_mapping_profile.json",
          "default_holdings_mapping_profile.json",
          "default_instance_mapping_profile.json"
      );

  private static final List<String> JOB_PROFILES =
      List.of(
          "default_authority_job_profile.json",
          "default_holdings_job_profile.json",
          "default_instance_job_profile.json",
          "default_deleted_authority_job_profile.json"
      );
  public static final String TENANT_FOR_VIEWS = "myuniversity";

  private JobProfileEntityRepository jobProfileEntityRepository;
  private MappingProfileEntityRepository mappingProfileEntityRepository;
  private ConfigurationService configurationService;
  private TimerService timerService;

  /**
   * Constructs a DataExportTenantService with required dependencies.
   *
   * @param jdbcTemplate JDBC template
   * @param context Folio execution context
   * @param folioSpringLiquibase Liquibase integration
   * @param jobProfileEntityRepository Job profile repository
   * @param mappingProfileEntityRepository Mapping profile repository
   * @param configurationService Configuration service
   * @param timerService Timer service
   */
  @Autowired
  public DataExportTenantService(
      JdbcTemplate jdbcTemplate,
      FolioExecutionContext context,
      FolioSpringLiquibase folioSpringLiquibase,
      JobProfileEntityRepository jobProfileEntityRepository,
      MappingProfileEntityRepository mappingProfileEntityRepository,
      ConfigurationService configurationService,
      TimerService timerService
  ) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.jobProfileEntityRepository = jobProfileEntityRepository;
    this.mappingProfileEntityRepository = mappingProfileEntityRepository;
    this.configurationService = configurationService;
    this.timerService = timerService;
  }

  /**
   * Loads reference data for the tenant, including mapping and job profiles.
   */
  @Override
  public void loadReferenceData() {
    log.info("Start to load reference data");
    loadMappingProfiles();
    loadJobProfiles();
  }

  /**
   * Creates or updates the tenant, sets up configuration and timers.
   *
   * @param tenantAttributes Tenant attributes.
   */
  @Override
  public synchronized void createOrUpdateTenant(TenantAttributes tenantAttributes) {
    setupTenantForViews();
    super.createOrUpdateTenant(tenantAttributes);
    setupConfigEntryInventoryRecordLink();
    log.info("Loading configuration");
    loadConfiguration();
    timerService.updateCleanUpFilesTimerIfRequired();
  }

  /**
   * Sets up the tenant value for views.
   */
  private void setupTenantForViews() {
    var tenant = super.context.getTenantId();
    log.info("Tenant value for views is {}", tenant);
    System.setProperty(TENANT_FOR_VIEWS, tenant);
  }

  /**
   * Loads all mapping profiles from resource files.
   */
  private void loadMappingProfiles() {
    MAPPING_PROFILES.forEach(
        mappingProfile ->
            loadMappingProfile("/data/mapping-profiles/" + mappingProfile)
    );
  }

  /**
   * Loads a single mapping profile from a resource file.
   *
   * @param mappingProfilePath Path to the mapping profile resource.
   */
  private void loadMappingProfile(String mappingProfilePath) {
    var mapper = new ObjectMapper();
    try (
        InputStream is =
            DataExportTenantService.class.getResourceAsStream(mappingProfilePath)
    ) {
      var mappingProfile = mapper.readValue(is, MappingProfile.class);
      mappingProfileEntityRepository.save(
          MappingProfileEntity.fromMappingProfile(mappingProfile)
      );
    } catch (Exception e) {
      log.error(
          "Error loading mapping profile {} : {}",
          FilenameUtils.getBaseName(mappingProfilePath),
          e.getMessage()
      );
    }
  }

  /**
   * Loads all job profiles from resource files.
   */
  private void loadJobProfiles() {
    JOB_PROFILES.forEach(
        jobProfile ->
            loadJobProfile("/data/job-profiles/" + jobProfile)
    );
  }

  /**
   * Loads a single job profile from a resource file.
   *
   * @param jobProfilePath Path to the job profile resource.
   */
  private void loadJobProfile(String jobProfilePath) {
    var mapper = new ObjectMapper();
    try (
        InputStream is =
            DataExportTenantService.class.getResourceAsStream(jobProfilePath)
    ) {
      var jobProfile = mapper.readValue(is, JobProfile.class);
      jobProfileEntityRepository.save(
          JobProfileEntity.fromJobProfile(jobProfile)
      );
    } catch (Exception e) {
      log.error(
          "Error loading job profile {} : {}",
          FilenameUtils.getBaseName(jobProfilePath),
          e.getMessage()
      );
    }
  }

  /**
   * Loads configuration values for the tenant.
   */
  private void loadConfiguration() {
    setupDefaultSliceSizeValue();
  }

  /**
   * Sets up the default slice size configuration value.
   */
  private void setupDefaultSliceSizeValue() {
    log.info("Loading default slice size value...");
    var saved = configurationService.upsertConfiguration(
        new Config()
            .key(SlicerProcessor.SLICE_SIZE_KEY)
            .value(String.valueOf(SlicerProcessor.DEFAULT_SLICE_SIZE))
    );
    log.info("Loaded default slice size value: {}", saved.getValue());
  }

  /**
   * Sets up the inventory record link configuration entry.
   */
  private void setupConfigEntryInventoryRecordLink() {
    log.info("Loading inventory record link value...");
    var inventoryRecordLinkConfig =
        configurationService.produceInventoryRecordLinkBasedOnFolioHostConfigFromRemote();
    var saved = configurationService.upsertConfiguration(inventoryRecordLinkConfig);
    log.info("Loaded inventory record link value: {}", saved.getValue());
  }
}
