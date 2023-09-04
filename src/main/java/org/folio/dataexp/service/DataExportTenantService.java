package org.folio.dataexp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dataexp.domain.dto.JobProfile;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.entity.JobProfileEntity;
import org.folio.dataexp.domain.entity.MappingProfileEntity;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;


@Service
@Primary
@Log4j2
public class DataExportTenantService extends TenantService {

  private static final String MAPPING_PROFILES_PATH ="/data/mapping-profiles/";
  private static final List<String> MAPPING_PROFILES = List.of("default_authority_mapping_profile.json",
    "default_holdings_mapping_profile.json", "default_instance_mapping_profile.json");

  private static final String JOB_PROFILES_PATH = "/data/job-profiles/";
  private static final List<String> JOB_PROFILES = List.of("default_authority_job_profile.json",
    "default_holdings_job_profile.json", "default_instance_job_profile.json");

  @Autowired
  private JobProfileEntityRepository jobProfileEntityRepository;
  @Autowired
  private MappingProfileEntityRepository mappingProfileEntityRepository;

  private final ObjectMapper mapper = new ObjectMapper();

  public DataExportTenantService(JdbcTemplate jdbcTemplate, FolioExecutionContext context, FolioSpringLiquibase folioSpringLiquibase) {
    super(jdbcTemplate, context, folioSpringLiquibase);
  }

  @Override
  public void loadReferenceData() {
    log.info("Start to load reference data");
    loadMappingProfiles();
    loadJobProfiles();
  }

  private void loadMappingProfiles() {
    MAPPING_PROFILES.forEach(mappingProfile -> loadMappingProfile(MAPPING_PROFILES_PATH + mappingProfile));
  }
  private void loadMappingProfile(String path) {
    try (InputStream is =
           DataExportTenantService.class.getResourceAsStream(path)) {
      var mappingProfile = mapper.readValue(is, MappingProfile.class);
      var mappingProfileEntity = MappingProfileEntity.builder()
        .id(mappingProfile.getId())
        .creationDate(LocalDateTime.now())
        .mappingProfile(mappingProfile)
        .createdBy(mappingProfile.getMetadata().getCreatedByUserId()).build();
      mappingProfileEntityRepository.save(mappingProfileEntity);
    } catch (Exception e) {
      log.error("Error loading mapping profile {} : {}", FilenameUtils.getBaseName(path), e.getMessage());
    }
  }

  private void loadJobProfiles() {
    JOB_PROFILES.forEach(jobProfile -> loadJobProfile(JOB_PROFILES_PATH + jobProfile));
  }

  private void loadJobProfile(String path) {
    try (InputStream is =
           DataExportTenantService.class.getResourceAsStream(path)) {
      var jobProfile = mapper.readValue(is, JobProfile.class);
      var jobProfileEntity = JobProfileEntity.builder()
        .id(jobProfile.getId())
        .creationDate(LocalDateTime.now())
        .jobProfile(jobProfile)
        .name(jobProfile.getName())
        .createdBy(jobProfile.getMetadata().getCreatedByUserId())
        .mappingProfileId(jobProfile.getMappingProfileId()).build();
      jobProfileEntityRepository.save(jobProfileEntity);
    } catch (Exception e) {
      log.error("Error loading job profile {} : {}", FilenameUtils.getBaseName(path), e.getMessage());
    }
  }
}
