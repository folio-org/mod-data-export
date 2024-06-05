package org.folio.dataexp.service;

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
import org.folio.dataexp.domain.dto.Campuses;
import org.folio.dataexp.domain.dto.ContributorNameTypes;
import org.folio.dataexp.domain.dto.ElectronicAccessRelationships;
import org.folio.dataexp.domain.dto.IdentifierTypes;
import org.folio.dataexp.domain.dto.InstanceFormats;
import org.folio.dataexp.domain.dto.InstanceTypes;
import org.folio.dataexp.domain.dto.Institutions;
import org.folio.dataexp.domain.dto.IssuanceModes;
import org.folio.dataexp.domain.dto.ItemNoteTypes;
import org.folio.dataexp.domain.dto.Libraries;
import org.folio.dataexp.domain.dto.LoanTypes;
import org.folio.dataexp.domain.dto.Locations;
import org.folio.dataexp.domain.dto.MaterialTypes;
import org.folio.dataexp.domain.dto.User;
import org.folio.dataexp.repository.ErrorLogEntityCqlRepository;
import org.folio.dataexp.repository.JobExecutionEntityCqlRepository;
import org.folio.dataexp.repository.JobExecutionExportFilesEntityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

abstract class ServiceInitializer extends BaseDataExportInitializer {

  @Autowired
  protected DataExportTenantService dataExportTenantService;
  @Autowired
  protected ErrorLogEntityCqlRepository errorLogEntityCqlRepository;
  @Autowired
  protected JobExecutionEntityCqlRepository jobExecutionEntityCqlRepository;
  @Autowired
  protected JobExecutionExportFilesEntityRepository jobExecutionExportFilesEntityRepository;

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
  @MockBean
  private ConsortiaService consortiaService;

  protected void handleReferenceData() {
    var user = new User();
    var personal = new User.Personal();
    personal.setFirstName("testuser");
    personal.setLastName("testuser");
    user.setPersonal(personal);
    when(userClient.getUserById(any(String.class))).thenReturn(user);
    when(alternativeTitleTypesClient.getAlternativeTitleTypes(any(Long.class))).thenReturn(new AlternativeDataTypes());
    when(callNumberTypesClient.getCallNumberTypes(any(Long.class))).thenReturn(new org.folio.dataexp.domain.dto.CallNumberTypes());
    when(contributorNameTypesClient.getContributorNameTypes(any(Long.class))).thenReturn(new ContributorNameTypes());
    when(electronicAccessRelationshipsClient.getElectronicAccessRelationships(any(Long.class))).thenReturn(new ElectronicAccessRelationships());
    when(holdingsNoteTypesClient.getHoldingsNoteTypes(any(Long.class))).thenReturn(new org.folio.dataexp.domain.dto.HoldingsNoteTypes());
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
    when(consortiaService.getCentralTenantId()).thenReturn("central");
  }
}
