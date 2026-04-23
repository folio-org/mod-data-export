package org.folio.dataexp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.folio.dataexp.BaseDataExportInitializerIT;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies that default Linked Data job/mapping profiles inserted by Liquibase migrations ({@code
 * add_default_linked_data_mapping_profile}, {@code add_default_linked_data_job_profile}) are
 * present in the database after tenant initialization.
 */
class DefaultProfilesMigrationIT extends BaseDataExportInitializerIT {

  private static final UUID LINKED_DATA_JOB_PROFILE_ID =
      UUID.fromString("42ca0945-f66c-4bc1-8d1a-7aa8b2e4483a");
  private static final UUID LINKED_DATA_MAPPING_PROFILE_ID =
      UUID.fromString("f8b400da-6a0c-4058-be10-cece93265c32");

  @Autowired private JobProfileEntityRepository jobProfileEntityRepository;
  @Autowired private MappingProfileEntityRepository mappingProfileEntityRepository;


  @Test
  void liquibaseShouldInsertDefaultLinkedDataMappingProfile() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var entity =
          mappingProfileEntityRepository
              .findById(LINKED_DATA_MAPPING_PROFILE_ID)
              .orElseThrow(
                  () ->
                      new AssertionError(
                          "Default linked data mapping profile is expected to be created by "
                              + "Liquibase migration 'add_default_linked_data_mapping_profile'"));
      assertEquals(LINKED_DATA_MAPPING_PROFILE_ID, entity.getId());
    }
  }

  @Test
  void liquibaseShouldInsertDefaultLinkedDataJobProfile() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var entity =
          jobProfileEntityRepository
              .findById(LINKED_DATA_JOB_PROFILE_ID)
              .orElseThrow(
                  () ->
                      new AssertionError(
                          "Default linked data job profile is expected to be created by "
                              + "Liquibase migration 'add_default_linked_data_job_profile'"));
      assertEquals(LINKED_DATA_JOB_PROFILE_ID, entity.getId());
      assertEquals(LINKED_DATA_MAPPING_PROFILE_ID, entity.getMappingProfileId());
      assertTrue(
          mappingProfileEntityRepository.existsById(LINKED_DATA_MAPPING_PROFILE_ID),
          "Referenced linked data mapping profile must exist (FK).");
    }
  }
}
