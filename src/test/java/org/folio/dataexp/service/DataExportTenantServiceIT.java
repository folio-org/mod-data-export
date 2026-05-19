package org.folio.dataexp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;
import org.folio.dataexp.BaseDataExportInitializerIT;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DataExportTenantServiceIT extends BaseDataExportInitializerIT {

  private static final List<UUID> REFERENCE_DATA_JOB_PROFILE_IDS =
      List.of(
          UUID.fromString("6f7f3cd7-9f24-42eb-ae91-91af1cd54d0a"), // default instance
          UUID.fromString("56944b1c-f3f9-475b-bed0-7387c33620ce"), // default authority
          UUID.fromString("5e9835fc-0e51-44c8-8a47-f7b8fce35da7"), // default holdings
          UUID.fromString("2c9be114-6d35-4408-adac-9ead35f51a27")); // deleted authority
  private static final List<UUID> REFERENCE_DATA_MAPPING_PROFILE_IDS =
      List.of(
          UUID.fromString("25d81cbe-9686-11ea-bb37-0242ac130002"), // default instance
          UUID.fromString("1ef7d0ac-f0a8-42b5-bbbb-c7e249009c13"), // default holdings
          UUID.fromString("5d636597-a59d-4391-a270-4e79d5ba70e3")); // default authority

  @Autowired private DataExportTenantService dataExportTenantService;
  @Autowired private JobProfileEntityRepository jobProfileEntityRepository;
  @Autowired private MappingProfileEntityRepository mappingProfileEntityRepository;

  @Test
  void loadReferenceDataTest() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      dataExportTenantService.loadReferenceData();

      var expectedDefaultJobProfileAmount = 5;
      var expectedDefaultMappingProfileAmount = 4;

      assertEquals(expectedDefaultJobProfileAmount, jobProfileEntityRepository.count());
      assertEquals(expectedDefaultMappingProfileAmount, mappingProfileEntityRepository.count());

      // Cleanup only reference-data profiles to avoid removing rows
      // inserted by Liquibase migrations (shared static Postgres container
      // is reused across IT classes within a single Maven build).
      // Job profiles must be deleted first because of FK to mapping profiles.
      REFERENCE_DATA_JOB_PROFILE_IDS.forEach(jobProfileEntityRepository::deleteById);
      REFERENCE_DATA_MAPPING_PROFILE_IDS.forEach(mappingProfileEntityRepository::deleteById);
    }
  }
}
