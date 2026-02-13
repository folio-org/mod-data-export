package org.folio.dataexp.service.export.strategies;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dataexp.util.ErrorCode.ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION;
import static org.folio.dataexp.util.ErrorCode.ERROR_MESSAGE_UUID_IS_SET_TO_DELETION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.client.ConsortiaClient;
import org.folio.dataexp.domain.dto.ExportRequest;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.folio.dataexp.domain.dto.UserTenant;
import org.folio.dataexp.domain.dto.UserTenantCollection;
import org.folio.dataexp.service.JobExecutionService;
import org.folio.dataexp.service.logs.ErrorLogService;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AuthorityExportStrategyTest extends BaseDataExportInitializer {

  private static final UUID LOCAL_AUTHORITY_UUID =
      UUID.fromString("4a090b0f-9da3-40f1-ab17-33d6a1e3abae");
  private static final UUID CENTRAL_AUTHORITY_UUID =
      UUID.fromString("26be956e-98f2-11ee-b9d1-0242ac120002");
  private static final UUID LOCAL_MARC_AUTHORITY_UUID =
      UUID.fromString("17eed93e-f9e2-4cb2-a52b-e9155acfc119");
  private static final UUID CENTRAL_MARC_AUTHORITY_UUID =
      UUID.fromString("ed0ad74c-98f1-11ee-b9d1-0242ac120002");
  private static final UUID LOCAL_MARC_AUTHORITY_DELETED_UUID_1 =
      UUID.fromString("34090b0f-9da3-40f1-ab17-33d6a1e3abae");
  private static final UUID LOCAL_MARC_AUTHORITY_DELETED_UUID_2 =
      UUID.fromString("45090b0f-9da3-40f1-ab17-33d6a1e3abae");

  @Autowired private AuthorityExportStrategy authorityExportStrategy;

  @Autowired private ErrorLogService errorLogService;

  @MockitoBean private ConsortiaClient consortiaClient;
  @MockitoBean private JobExecutionService jobExecutionService;

  @Test
  void shouldReturnOneLocalRecord() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      var localAuthorityIds = new HashSet<UUID>();
      localAuthorityIds.add(LOCAL_AUTHORITY_UUID);
      var mappingProfile = new MappingProfile();
      mappingProfile.setDefault(true);
      var marcRecords =
          authorityExportStrategy.getMarcRecords(
              localAuthorityIds,
              mappingProfile,
              new ExportRequest().jobProfileId(DEFAULT_AUTHORITY_JOB_PROFILE),
              UUID.randomUUID());

      assertThat(marcRecords).hasSize(1);
      assertEquals(LOCAL_MARC_AUTHORITY_UUID, marcRecords.get(0).getId());
    }
  }

  @Test
  void shouldReturnOneSharedRecord() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      handleCentralTenant();

      var centralAuthorityIds = new HashSet<UUID>();
      centralAuthorityIds.add(CENTRAL_AUTHORITY_UUID);
      var mappingProfile = new MappingProfile();
      mappingProfile.setDefault(true);
      var marcRecords =
          authorityExportStrategy.getMarcRecords(
              centralAuthorityIds,
              mappingProfile,
              new ExportRequest().jobProfileId(UUID.randomUUID()),
              UUID.randomUUID());

      assertThat(marcRecords).hasSize(1);
      assertEquals(CENTRAL_MARC_AUTHORITY_UUID, marcRecords.get(0).getId());
    }
  }

  @Test
  void shouldReturnOneSharedAndOneLocalRecord() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      handleCentralTenant();

      var centralAuthorityIds = new HashSet<UUID>();
      centralAuthorityIds.add(CENTRAL_AUTHORITY_UUID);
      centralAuthorityIds.add(LOCAL_AUTHORITY_UUID);
      var mappingProfile = new MappingProfile();
      mappingProfile.setDefault(true);
      var marcRecords =
          authorityExportStrategy.getMarcRecords(
              centralAuthorityIds,
              mappingProfile,
              new ExportRequest().jobProfileId(DEFAULT_AUTHORITY_JOB_PROFILE),
              UUID.randomUUID());

      assertThat(marcRecords).hasSize(2);
      assertEquals(LOCAL_MARC_AUTHORITY_UUID, marcRecords.get(0).getId());
      assertEquals(CENTRAL_MARC_AUTHORITY_UUID, marcRecords.get(1).getId());
    }
  }

  @Test
  void shouldReturnEmptyList_ifIdNotFound() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      handleCentralTenant();

      var notFoundUuid = UUID.randomUUID();
      var centralAuthorityIds = new HashSet<UUID>();
      centralAuthorityIds.add(notFoundUuid);
      var mappingProfile = new MappingProfile();
      mappingProfile.setDefault(true);
      var marcRecords =
          authorityExportStrategy.getMarcRecords(
              centralAuthorityIds,
              mappingProfile,
              new ExportRequest().jobProfileId(UUID.randomUUID()),
              UUID.randomUUID());

      assertThat(marcRecords).isEmpty();
    }
  }

  @Test
  void shouldReturnOneRecordFromLocalAndOneNotFoundIfOneIdNotFound() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      handleCentralTenant();

      var notFoundUuid = UUID.randomUUID();
      var centralAuthorityIds = new HashSet<UUID>();
      centralAuthorityIds.add(notFoundUuid);
      centralAuthorityIds.add(LOCAL_AUTHORITY_UUID);
      var mappingProfile = new MappingProfile();
      mappingProfile.setDefault(true);
      var marcRecords =
          authorityExportStrategy.getMarcRecords(
              centralAuthorityIds,
              mappingProfile,
              new ExportRequest().jobProfileId(DEFAULT_AUTHORITY_JOB_PROFILE),
              UUID.randomUUID());

      assertThat(marcRecords).hasSize(1);
      assertEquals(LOCAL_MARC_AUTHORITY_UUID, marcRecords.get(0).getId());
    }
  }

  @Test
  void shouldReturnErrorResultForGetGeneratedMarcIfMarcsDoNotExist() {
    var notExistUuid = UUID.randomUUID();
    var marcRecords =
        authorityExportStrategy.getGeneratedMarc(
            Set.of(notExistUuid),
            new MappingProfile(),
            new ExportRequest(),
            UUID.randomUUID(),
            new ExportStrategyStatistic(new ExportedRecordsListener(null, 1000, null)));
    assertEquals(1, marcRecords.getNotExistIds().size());
    assertEquals(1, marcRecords.getFailedIds().size());
  }

  @Test
  void shouldExportDeletedIfJobProfileIsDeleted() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      handleCentralTenant();

      var mappingProfile = new MappingProfile();
      mappingProfile.setDefault(true);
      var deletedAuthorities = new HashSet<UUID>();
      deletedAuthorities.add(LOCAL_MARC_AUTHORITY_DELETED_UUID_1);
      deletedAuthorities.add(LOCAL_MARC_AUTHORITY_DELETED_UUID_2);
      var marcRecords =
          authorityExportStrategy.getMarcRecords(
              deletedAuthorities,
              mappingProfile,
              new ExportRequest().jobProfileId(DEFAULT_DELETED_AUTHORITY_JOB_PROFILE),
              UUID.randomUUID());
      assertThat(marcRecords).hasSize(2);
    }
  }

  @Test
  void shouldExportOnlyDeletedIfJobProfileIsDeleted() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      handleCentralTenant();

      var mappingProfile = new MappingProfile();
      mappingProfile.setDefault(true);
      var deletedAuthorities = new HashSet<UUID>();
      deletedAuthorities.add(LOCAL_MARC_AUTHORITY_DELETED_UUID_1);
      deletedAuthorities.add(LOCAL_AUTHORITY_UUID);
      var jobExecutionId = UUID.randomUUID();

      when(jobExecutionService.getById(jobExecutionId))
          .thenReturn(
              new org.folio.dataexp.domain.dto.JobExecution()
                  .id(jobExecutionId)
                  .jobProfileId(DEFAULT_DELETED_AUTHORITY_JOB_PROFILE));
      var marcRecords =
          authorityExportStrategy.getMarcRecords(
              deletedAuthorities,
              mappingProfile,
              new ExportRequest().jobProfileId(DEFAULT_DELETED_AUTHORITY_JOB_PROFILE),
              jobExecutionId);
      assertThat(marcRecords).hasSize(1);
      assertEquals(LOCAL_MARC_AUTHORITY_DELETED_UUID_1, marcRecords.get(0).getExternalId());
      var errors = errorLogService.getByQuery(format("(jobExecutionId==%s)", jobExecutionId));
      assertThat(errors).hasSize(1);
      var error = errors.get(0);
      assertEquals(
          ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION.getDescription(),
          error.getErrorMessageValues().get(0));
      assertEquals(
          ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION.getCode(), error.getErrorMessageCode());
    }
  }

  @Test
  void shouldExportNothingIfJobProfileIsDeletedAndRecordIsActual() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      handleCentralTenant();
      var jobExecutionId = UUID.randomUUID();
      when(jobExecutionService.getById(jobExecutionId))
          .thenReturn(
              new org.folio.dataexp.domain.dto.JobExecution()
                  .id(jobExecutionId)
                  .jobProfileId(DEFAULT_DELETED_AUTHORITY_JOB_PROFILE));

      var mappingProfile = new MappingProfile();
      mappingProfile.setDefault(true);
      var deletedAuthorities = new HashSet<UUID>();
      deletedAuthorities.add(LOCAL_AUTHORITY_UUID);
      var marcRecords =
          authorityExportStrategy.getMarcRecords(
              deletedAuthorities,
              mappingProfile,
              new ExportRequest().jobProfileId(DEFAULT_DELETED_AUTHORITY_JOB_PROFILE),
              jobExecutionId);
      assertThat(marcRecords).isEmpty();
      var errors = errorLogService.getByQuery(format("(jobExecutionId==%s)", jobExecutionId));
      assertThat(errors).hasSize(1);
      var error = errors.getFirst();
      assertEquals(
          ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION.getDescription(),
          error.getErrorMessageValues().getFirst());
      assertEquals(
          ERROR_MESSAGE_USED_ONLY_FOR_SET_TO_DELETION.getCode(), error.getErrorMessageCode());
    }
  }

  @Test
  void shouldExportOnlyActualIfJobProfileIsDefault() {
    try (var context = new FolioExecutionContextSetter(folioExecutionContext)) {
      handleCentralTenant();

      var mappingProfile = new MappingProfile();
      mappingProfile.setDefault(true);
      var deletedAuthorities = new HashSet<UUID>();
      deletedAuthorities.add(LOCAL_AUTHORITY_UUID);
      deletedAuthorities.add(LOCAL_MARC_AUTHORITY_DELETED_UUID_1);
      var jobExecutionId = UUID.randomUUID();

      when(jobExecutionService.getById(jobExecutionId))
          .thenReturn(
              new org.folio.dataexp.domain.dto.JobExecution()
                  .id(jobExecutionId)
                  .jobProfileId(DEFAULT_AUTHORITY_JOB_PROFILE));
      var marcRecords =
          authorityExportStrategy.getMarcRecords(
              deletedAuthorities,
              mappingProfile,
              new ExportRequest().jobProfileId(DEFAULT_AUTHORITY_JOB_PROFILE),
              jobExecutionId);
      assertThat(marcRecords).hasSize(1);
      var errors = errorLogService.getByQuery(format("(jobExecutionId==%s)", jobExecutionId));
      assertThat(errors).hasSize(2);
      var error = errors.get(0);
      assertEquals(
          format(
              ERROR_MESSAGE_UUID_IS_SET_TO_DELETION.getDescription(),
              LOCAL_MARC_AUTHORITY_DELETED_UUID_1),
          error.getErrorMessageValues().get(0));
      assertEquals(ERROR_MESSAGE_UUID_IS_SET_TO_DELETION.getCode(), error.getErrorMessageCode());
    }
  }

  private void handleCentralTenant() {
    var userTenantCollection = new UserTenantCollection();
    var userTenants = new ArrayList<UserTenant>();
    var centralUserTenant = new UserTenant();
    centralUserTenant.setCentralTenantId("central");
    userTenants.add(centralUserTenant);
    userTenantCollection.setUserTenants(userTenants);
    when(consortiaClient.getUserTenantCollection()).thenReturn(userTenantCollection);
  }
}
