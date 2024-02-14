package org.folio.dataexp.service;

import lombok.SneakyThrows;
import org.folio.dataexp.BaseDataExportInitializer;
import org.folio.dataexp.client.AlternativeTitleTypesClient;
import org.folio.dataexp.client.CallNumberTypesClient;
import org.folio.dataexp.client.ContributorNameTypesClient;
import org.folio.dataexp.client.ElectronicAccessRelationshipsClient;
import org.folio.dataexp.client.HoldingsNoteTypesClient;
import org.folio.dataexp.client.IdentifierTypesClient;
import org.folio.dataexp.client.InstanceFormatsClient;
import org.folio.dataexp.client.InstanceTypesClient;
import org.folio.dataexp.client.IssuanceModesClient;
import org.folio.dataexp.client.ItemNoteTypesClient;
import org.folio.dataexp.client.LoanTypesClient;
import org.folio.dataexp.client.LocationUnitsClient;
import org.folio.dataexp.client.LocationsClient;
import org.folio.dataexp.client.MaterialTypesClient;
import org.folio.dataexp.client.NatureOfContentTermsClient;
import org.folio.dataexp.client.UserClient;
import org.folio.dataexp.domain.dto.AlternativeDataTypes;
import org.folio.dataexp.domain.dto.CallNumberTypes;
import org.folio.dataexp.domain.dto.Campuses;
import org.folio.dataexp.domain.dto.ContributorNameTypes;
import org.folio.dataexp.domain.dto.ElectronicAccessRelationships;
import org.folio.dataexp.domain.dto.ExportAllRequest;
import org.folio.dataexp.domain.dto.HoldingsNoteTypes;
import org.folio.dataexp.domain.dto.IdentifierTypes;
import org.folio.dataexp.domain.dto.InstanceFormats;
import org.folio.dataexp.domain.dto.InstanceTypes;
import org.folio.dataexp.domain.dto.Institutions;
import org.folio.dataexp.domain.dto.IssuanceModes;
import org.folio.dataexp.domain.dto.ItemNoteTypes;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.Libraries;
import org.folio.dataexp.domain.dto.LoanTypes;
import org.folio.dataexp.domain.dto.Locations;
import org.folio.dataexp.domain.dto.MaterialTypes;
import org.folio.dataexp.domain.dto.User;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.dataexp.repository.JobExecutionEntityCqlRepository;
import org.folio.dataexp.repository.JobProfileEntityRepository;
import org.folio.dataexp.repository.MappingProfileEntityRepository;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class DataExportAllServiceTest extends BaseDataExportInitializer {

  @Autowired
  private DataExportAllService dataExportAllService;

  @Autowired
  private JobExecutionEntityCqlRepository jobExecutionEntityCqlRepository;

  @Autowired
  private JobProfileEntityRepository jobProfileEntityRepository;

  @Autowired
  private MappingProfileEntityRepository mappingProfileEntityRepository;

  @Autowired
  private DataExportTenantService dataExportTenantService;

  @Autowired
  private ErrorLogEntityCqlRepository errorLogEntityCqlRepository;

  @MockBean
  private UserClient userClient;
  @MockBean
  private AlternativeTitleTypesClient alternativeTitleTypesClient;
  @MockBean
  private CallNumberTypesClient callNumberTypesClient;
  @MockBean
  private ContributorNameTypesClient contributorNameTypesClient;
  @MockBean
  private ElectronicAccessRelationshipsClient electronicAccessRelationshipsClient;
  @MockBean
  private HoldingsNoteTypesClient holdingsNoteTypesClient;
  @MockBean
  private IdentifierTypesClient identifierTypesClient;
  @MockBean
  private InstanceFormatsClient instanceFormatsClient;
  @MockBean
  private InstanceTypesClient instanceTypesClient;
  @MockBean
  private ItemNoteTypesClient itemNoteTypesClient;
  @MockBean
  private LoanTypesClient loanTypesClient;
  @MockBean
  private LocationsClient locationsClient;
  @MockBean
  private LocationUnitsClient locationUnitsClient;
  @MockBean
  private MaterialTypesClient materialTypesClient;
  @MockBean
  private NatureOfContentTermsClient natureOfContentTermsClient;
  @MockBean
  private IssuanceModesClient issuanceModesClient;

  @Test
  @SneakyThrows
  void exportAllInstancesNoErrorsTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();
      var exportAllRequest = new ExportAllRequest();
      dataExportAllService.postDataExportAll(exportAllRequest);
      await().atMost(2, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        assertEquals(2, jobExecution.getJobExecution().getProgress().getTotal());
      });
    }
  }

  @Test
  @SneakyThrows
  void exportAllHoldingsNoErrorsTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();
      var exportAllRequest = new ExportAllRequest().idType(ExportAllRequest.IdTypeEnum.HOLDING)
        .jobProfileId(DEFAULT_HOLDINGS_JOB_PROFILE);
      dataExportAllService.postDataExportAll(exportAllRequest);
      await().atMost(2, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        assertEquals(2, jobExecution.getJobExecution().getProgress().getTotal());
      });
    }
  }

  @Test
  @SneakyThrows
  void exportAllAuthorityNoErrorsTest() {
    try (var context =  new FolioExecutionContextSetter(folioExecutionContext)) {
      errorLogEntityCqlRepository.deleteAll();
      dataExportTenantService.loadReferenceData();
      handleReferenceData();
      var exportAllRequest = new ExportAllRequest().idType(ExportAllRequest.IdTypeEnum.AUTHORITY)
        .jobProfileId(DEFAULT_AUTHORITY_JOB_PROFILE);
      dataExportAllService.postDataExportAll(exportAllRequest);
      await().atMost(2, SECONDS).untilAsserted(() -> {
        var jobExecutions = jobExecutionEntityCqlRepository.findAll();
        var errors = errorLogEntityCqlRepository.findAll();
        assertThat(errors).isEmpty();
        assertEquals(1, jobExecutions.size());
        var jobExecution = jobExecutions.get(0);
        assertEquals(JobExecution.StatusEnum.COMPLETED, jobExecution.getStatus());
        assertEquals(1, jobExecution.getJobExecution().getProgress().getTotal());
      });
    }
  }

  private void handleReferenceData() {
    var user = new User();
    var personal = new User.Personal();
    personal.setFirstName("testuser");
    personal.setLastName("testuser");
    user.setPersonal(personal);
    when(userClient.getUserById(any(String.class))).thenReturn(user);
    when(alternativeTitleTypesClient.getAlternativeTitleTypes(any(Long.class))).thenReturn(new AlternativeDataTypes());
    when(callNumberTypesClient.getCallNumberTypes(any(Long.class))).thenReturn(new CallNumberTypes());
    when(contributorNameTypesClient.getContributorNameTypes(any(Long.class))).thenReturn(new ContributorNameTypes());
    when(electronicAccessRelationshipsClient.getElectronicAccessRelationships(any(Long.class))).thenReturn(new ElectronicAccessRelationships());
    when(holdingsNoteTypesClient.getHoldingsNoteTypes(any(Long.class))).thenReturn(new HoldingsNoteTypes());
    when(identifierTypesClient.getIdentifierTypes(any(Long.class))).thenReturn(new IdentifierTypes());
    when(instanceFormatsClient.getInstanceFormats(any(Long.class))).thenReturn(new InstanceFormats());
    when(instanceTypesClient.getInstanceTypes(any(Long.class))).thenReturn(new InstanceTypes());
    when(itemNoteTypesClient.getItemNoteTypes(any(Long.class))).thenReturn(new ItemNoteTypes());
    when(loanTypesClient.getLoanTypes(any(Long.class))).thenReturn(new LoanTypes());
    when(locationsClient.getLocations(any(Long.class))).thenReturn(new Locations());
    when(locationUnitsClient.getCampuses(any(Long.class))).thenReturn(new Campuses());
    when(locationUnitsClient.getLibraries(any(Long.class))).thenReturn(new Libraries());
    when(locationUnitsClient.getInstitutions(any(Long.class))).thenReturn(new Institutions());
    when(materialTypesClient.getMaterialTypes(any(Long.class))).thenReturn(new MaterialTypes());
    when(natureOfContentTermsClient.getNatureOfContentTerms(any(Long.class))).thenReturn(new org.folio.dataexp.domain.dto.NatureOfContentTerms());
    when(issuanceModesClient.getIssuanceModes(any(Long.class))).thenReturn(new IssuanceModes());
  }
}
