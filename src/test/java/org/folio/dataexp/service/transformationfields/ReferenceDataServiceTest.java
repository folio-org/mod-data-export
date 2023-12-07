package org.folio.dataexp.service.transformationfields;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.folio.dataexp.domain.dto.AlternativeDataTypes;
import org.folio.dataexp.domain.dto.Alternativetitletype;
import org.folio.dataexp.domain.dto.CallNumberType;
import org.folio.dataexp.domain.dto.CallNumberTypes;
import org.folio.dataexp.domain.dto.Campus;
import org.folio.dataexp.domain.dto.Campuses;
import org.folio.dataexp.domain.dto.ContributorNameType;
import org.folio.dataexp.domain.dto.ContributorNameTypes;
import org.folio.dataexp.domain.dto.ElectronicAccessRelationship;
import org.folio.dataexp.domain.dto.ElectronicAccessRelationships;
import org.folio.dataexp.domain.dto.HoldingsNoteType;
import org.folio.dataexp.domain.dto.HoldingsNoteTypes;
import org.folio.dataexp.domain.dto.IdentifierType;
import org.folio.dataexp.domain.dto.IdentifierTypes;
import org.folio.dataexp.domain.dto.InstanceFormat;
import org.folio.dataexp.domain.dto.InstanceFormats;
import org.folio.dataexp.domain.dto.InstanceType;
import org.folio.dataexp.domain.dto.InstanceTypes;
import org.folio.dataexp.domain.dto.Institution;
import org.folio.dataexp.domain.dto.Institutions;
import org.folio.dataexp.domain.dto.IssuanceModes;
import org.folio.dataexp.domain.dto.ItemNoteType;
import org.folio.dataexp.domain.dto.ItemNoteTypes;
import org.folio.dataexp.domain.dto.Libraries;
import org.folio.dataexp.domain.dto.Library;
import org.folio.dataexp.domain.dto.LoanType;
import org.folio.dataexp.domain.dto.LoanTypes;
import org.folio.dataexp.domain.dto.Location;
import org.folio.dataexp.domain.dto.Locations;
import org.folio.dataexp.domain.dto.MaterialType;
import org.folio.dataexp.domain.dto.MaterialTypes;
import org.folio.dataexp.domain.dto.ModeOfIssuance;
import org.folio.dataexp.domain.dto.NatureOfContentTerm;
import org.folio.dataexp.domain.dto.NatureOfContentTerms;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ReferenceDataServiceTest {
  @Spy
  private ObjectMapper objectMapper = new ObjectMapper();
  @Mock
  private AlternativeTitleTypesClient alternativeTitleTypesClient;
  @Mock
  private CallNumberTypesClient callNumberTypesClient;
  @Mock
  private ContributorNameTypesClient contributorNameTypesClient;
  @Mock
  private ElectronicAccessRelationshipsClient electronicAccessRelationshipsClient;
  @Mock
  private HoldingsNoteTypesClient holdingsNoteTypesClient;
  @Mock
  private IdentifierTypesClient identifierTypesClient;
  @Mock
  private InstanceFormatsClient instanceFormatsClient;
  @Mock
  private InstanceTypesClient instanceTypesClient;
  @Mock
  private ItemNoteTypesClient itemNoteTypesClient;
  @Mock
  private LoanTypesClient loanTypesClient;
  @Mock
  private LocationsClient locationsClient;
  @Mock
  private LocationUnitsClient locationUnitsClient;
  @Mock
  private MaterialTypesClient materialTypesClient;
  @Mock
  private NatureOfContentTermsClient natureOfContentTermsClient;
  @Mock
  private IssuanceModesClient issuanceModesClient;
  @InjectMocks
  private ReferenceDataService referenceDataService;

  @Test
  void testGetAlternativeTitleTypes() {
    var id = UUID.randomUUID().toString();
    when(alternativeTitleTypesClient.getAlternativeTitleTypes(Integer.MAX_VALUE))
      .thenReturn(new AlternativeDataTypes()
        .alternativeTitleTypes(Collections.singletonList(new Alternativetitletype().id(id).name("name")))
        .totalRecords(1));
    var map = referenceDataService.getAlternativeTitleTypes();

    var actualValue = map.get(id);
    assertThat(actualValue.getMap()).containsEntry("name", "name");
  }

  @Test
  void testGetCallNumberTypes() {
    var id = UUID.randomUUID().toString();
    when(callNumberTypesClient.getCallNumberTypes(Integer.MAX_VALUE))
      .thenReturn(new CallNumberTypes()
        .callNumberTypes(Collections.singletonList(new CallNumberType()
          .id(id)
          .name("name")))
        .totalRecords(1));

    var map = referenceDataService.getCallNumberTypes();

    var actualValue = map.get(id);
    assertThat(actualValue.getMap()).containsEntry("name", "name");
  }

  @Test
  void testGetContributorNameTypes() {
    var id = UUID.randomUUID().toString();
    when(contributorNameTypesClient.getContributorNameTypes(Integer.MAX_VALUE))
      .thenReturn(new ContributorNameTypes()
        .contributorNameTypes(Collections.singletonList(new ContributorNameType()
          .id(id)
          .name("name")))
        .totalRecords(1));

    var map = referenceDataService.getContributorNameTypes();

    var actualValue = map.get(id);
    assertThat(actualValue.getMap()).containsEntry("name", "name");
  }

  @Test
  void testGetElectronicAccessRelationships() {
    var id = UUID.randomUUID().toString();
    when(electronicAccessRelationshipsClient.getElectronicAccessRelationships(Integer.MAX_VALUE))
      .thenReturn(new ElectronicAccessRelationships()
        .electronicAccessRelationships(Collections.singletonList(new ElectronicAccessRelationship()
          .id(id)
          .name("name")))
        .totalRecords(1));

    var map = referenceDataService.getElectronicAccessRelationships();

    var actualValue = map.get(id);
    assertThat(actualValue.getMap()).containsEntry("name", "name");
  }

  @Test
  void testGetHoldingsNoteTypes() {
    var id = UUID.randomUUID().toString();
    when(holdingsNoteTypesClient.getHoldingsNoteTypes(Integer.MAX_VALUE))
      .thenReturn(new HoldingsNoteTypes()
        .holdingsNoteTypes(Collections.singletonList(new HoldingsNoteType()
          .id(id)
          .name("name")))
        .totalRecords(1));

    var map = referenceDataService.getHoldingsNoteTypes();

    var actualValue = map.get(id);
    assertThat(actualValue.getMap()).containsEntry("name", "name");
  }

  @Test
  void testGetIdentifierTypes() {
    var id = UUID.randomUUID().toString();
    when(identifierTypesClient.getIdentifierTypes(Integer.MAX_VALUE))
      .thenReturn(new IdentifierTypes()
        .identifierTypes(Collections.singletonList(new IdentifierType()
          .id(id)
          .name("name")))
        .totalRecords(1));

    var map = referenceDataService.getIdentifierTypes();

    var actualValue = map.get(id);
    assertThat(actualValue.getMap()).containsEntry("name", "name");
  }

  @Test
  void testGetInstanceFormats() {
    var id = UUID.randomUUID().toString();
    when(instanceFormatsClient.getInstanceFormats(Integer.MAX_VALUE))
      .thenReturn(new InstanceFormats()
        .instanceFormats(Collections.singletonList(new InstanceFormat()
          .id(id)
          .name("name")))
        .totalRecords(1));

    var map = referenceDataService.getInstanceFormats();

    var actualValue = map.get(id);
    assertThat(actualValue.getMap()).containsEntry("name", "name");
  }

  @Test
  void testGetInstanceTypes() {
    var id = UUID.randomUUID().toString();
    when(instanceTypesClient.getInstanceTypes(Integer.MAX_VALUE))
      .thenReturn(new InstanceTypes()
        .instanceTypes(Collections.singletonList(new InstanceType()
          .id(id)
          .name("name")))
        .totalRecords(1));

    var map = referenceDataService.getInstanceTypes();

    var actualValue = map.get(id);
    assertThat(actualValue.getMap()).containsEntry("name", "name");
  }

  @Test
  void testGetItemNoteTypes() {
    var id = UUID.randomUUID().toString();
    when(itemNoteTypesClient.getItemNoteTypes(Integer.MAX_VALUE))
      .thenReturn(new ItemNoteTypes()
        .itemNoteTypes(Collections.singletonList(new ItemNoteType()
          .id(id)
          .name("name")))
        .totalRecords(1));

    var map = referenceDataService.getItemNoteTypes();

    var actualValue = map.get(id);
    assertThat(actualValue.getMap()).containsEntry("name", "name");
  }

  @Test
  void testGetLoanTypes() {
    var id = UUID.randomUUID().toString();
    when(loanTypesClient.getLoanTypes(Integer.MAX_VALUE))
      .thenReturn(new LoanTypes()
        .loantypes(Collections.singletonList(new LoanType()
          .id(id)
          .name("name")))
        .totalRecords(1));

    var map = referenceDataService.getLoanTypes();

    var actualValue = map.get(id);
    assertThat(actualValue.getMap()).containsEntry("name", "name");
  }

  @Test
  void testGetLocations() {
    var id = UUID.randomUUID().toString();
    when(locationsClient.getLocations(Integer.MAX_VALUE))
      .thenReturn(new Locations()
        .locations(Collections.singletonList(new Location()
          .id(id)
          .name("name")))
        .totalRecords(1));

    var map = referenceDataService.getLocations();

    var actualValue = map.get(id);
    assertThat(actualValue.getMap()).containsEntry("name", "name");
  }

  @Test
  void testGetCampuses() {
    var id = UUID.randomUUID().toString();
    when(locationUnitsClient.getCampuses(Integer.MAX_VALUE))
      .thenReturn(new Campuses()
        .campuses(Collections.singletonList(new Campus()
          .id(id)
          .name("name")))
        .totalRecords(1));

    var map = referenceDataService.getCampuses();

    var actualValue = map.get(id);
    assertThat(actualValue.getMap()).containsEntry("name", "name");
  }

  @Test
  void testGetInstitutions() {
    var id = UUID.randomUUID().toString();
    when(locationUnitsClient.getInstitutions(Integer.MAX_VALUE))
      .thenReturn(new Institutions()
        .institutions(Collections.singletonList(new Institution()
          .id(id)
          .name("name")))
        .totalRecords(1));

    var map = referenceDataService.getInstitutions();

    var actualValue = map.get(id);
    assertThat(actualValue.getMap()).containsEntry("name", "name");
  }

  @Test
  void testGetLibraries() {
    var id = UUID.randomUUID().toString();
    when(locationUnitsClient.getLibraries(Integer.MAX_VALUE))
      .thenReturn(new Libraries()
        .libraries(Collections.singletonList(new Library()
          .id(id)
          .name("name")))
        .totalRecords(1));

    var map = referenceDataService.getLibraries();

    var actualValue = map.get(id);
    assertThat(actualValue.getMap()).containsEntry("name", "name");
  }

  @Test
  void testGetMaterialTypes() {
    var id = UUID.randomUUID().toString();
    when(materialTypesClient.getMaterialTypes(Integer.MAX_VALUE))
      .thenReturn(new MaterialTypes()
        .mtypes(Collections.singletonList(new MaterialType()
          .id(id)
          .name("name")))
        .totalRecords(1));

    var map = referenceDataService.getMaterialTypes();

    var actualValue = map.get(id);
    assertThat(actualValue.getMap()).containsEntry("name", "name");
  }

  @Test
  void testGetNatureOfContentTerms() {
    var id = UUID.randomUUID().toString();
    when(natureOfContentTermsClient.getNatureOfContentTerms(Integer.MAX_VALUE))
      .thenReturn(new NatureOfContentTerms()
        .natureOfContentTerms(Collections.singletonList(new NatureOfContentTerm()
          .id(id)
          .name("name")))
        .totalRecords(1));

    var map = referenceDataService.getNatureOfContentTerms();

    var actualValue = map.get(id);
    assertThat(actualValue.getMap()).containsEntry("name", "name");
  }

  @Test
  void testGetIssuanceModes() {
    var id = UUID.randomUUID().toString();
    when(issuanceModesClient.getIssuanceModes(Integer.MAX_VALUE))
      .thenReturn(new IssuanceModes()
        .issuanceModes(Collections.singletonList(new ModeOfIssuance()
          .id(id)
          .name("name")))
        .totalRecords(1));

    var map = referenceDataService.getIssuanceModes();

    var actualValue = map.get(id);
    assertThat(actualValue.getMap()).containsEntry("name", "name");
  }

  @Test
  void testGetEmptyAlternativeTitleTypes() {
    when(alternativeTitleTypesClient.getAlternativeTitleTypes(Integer.MAX_VALUE))
      .thenReturn(new AlternativeDataTypes()
        .alternativeTitleTypes(Collections.emptyList())
        .totalRecords(0));

    var map = referenceDataService.getAlternativeTitleTypes();

    assertThat(map).isEmpty();
  }

  @Test
  void testGetEmptyCallNumberTypes() {
    when(callNumberTypesClient.getCallNumberTypes(Integer.MAX_VALUE))
      .thenReturn(new CallNumberTypes()
        .callNumberTypes(Collections.emptyList())
        .totalRecords(0));

    var map = referenceDataService.getCallNumberTypes();

    assertThat(map).isEmpty();
  }

  @Test
  void testGetEmptyContributorNameTypes() {
    when(contributorNameTypesClient.getContributorNameTypes(Integer.MAX_VALUE))
      .thenReturn(new ContributorNameTypes()
        .contributorNameTypes(Collections.emptyList())
        .totalRecords(0));

    var map = referenceDataService.getContributorNameTypes();

    assertThat(map).isEmpty();
  }

  @Test
  void testGetEmptyElectronicAccessRelationships() {
    when(electronicAccessRelationshipsClient.getElectronicAccessRelationships(Integer.MAX_VALUE))
      .thenReturn(new ElectronicAccessRelationships()
        .electronicAccessRelationships(Collections.emptyList())
        .totalRecords(0));

    var map = referenceDataService.getElectronicAccessRelationships();

    assertThat(map).isEmpty();
  }

  @Test
  void testGetEmptyHoldingsNoteTypes() {
    when(holdingsNoteTypesClient.getHoldingsNoteTypes(Integer.MAX_VALUE))
      .thenReturn(new HoldingsNoteTypes()
        .holdingsNoteTypes(Collections.emptyList())
        .totalRecords(0));

    var map = referenceDataService.getHoldingsNoteTypes();

    assertThat(map).isEmpty();
  }

  @Test
  void testGetEmptyIdentifierTypes() {
    when(identifierTypesClient.getIdentifierTypes(Integer.MAX_VALUE))
      .thenReturn(new IdentifierTypes()
        .identifierTypes(Collections.emptyList())
        .totalRecords(0));

    var map = referenceDataService.getIdentifierTypes();

    assertThat(map).isEmpty();
  }

  @Test
  void testGetEmptyInstanceFormats() {
    when(instanceFormatsClient.getInstanceFormats(Integer.MAX_VALUE))
      .thenReturn(new InstanceFormats()
        .instanceFormats(Collections.emptyList())
        .totalRecords(0));

    var map = referenceDataService.getInstanceFormats();

    assertThat(map).isEmpty();
  }

  @Test
  void testGetEmptyInstanceTypes() {
    when(instanceTypesClient.getInstanceTypes(Integer.MAX_VALUE))
      .thenReturn(new InstanceTypes()
        .instanceTypes(Collections.emptyList())
        .totalRecords(0));

    var map = referenceDataService.getInstanceTypes();

    assertThat(map).isEmpty();
  }

  @Test
  void testGetEmptyItemNoteTypes() {
    when(itemNoteTypesClient.getItemNoteTypes(Integer.MAX_VALUE))
      .thenReturn(new ItemNoteTypes()
        .itemNoteTypes(Collections.emptyList())
        .totalRecords(0));

    var map = referenceDataService.getItemNoteTypes();

    assertThat(map).isEmpty();
  }

  @Test
  void testGetEmptyLoanTypes() {
    when(loanTypesClient.getLoanTypes(Integer.MAX_VALUE))
      .thenReturn(new LoanTypes()
        .loantypes(Collections.emptyList())
        .totalRecords(0));

    var map = referenceDataService.getLoanTypes();

    assertThat(map).isEmpty();
  }

  @Test
  void testGetEmptyLocations() {
    when(locationsClient.getLocations(Integer.MAX_VALUE))
      .thenReturn(new Locations()
        .locations(Collections.emptyList())
        .totalRecords(0));

    var map = referenceDataService.getLocations();

    assertThat(map).isEmpty();
  }

  @Test
  void testGetEmptyCampuses() {
    when(locationUnitsClient.getCampuses(Integer.MAX_VALUE))
      .thenReturn(new Campuses()
        .campuses(Collections.emptyList())
        .totalRecords(0));

    var map = referenceDataService.getCampuses();

    assertThat(map).isEmpty();
  }

  @Test
  void testGetEmptyInstitutions() {
    when(locationUnitsClient.getInstitutions(Integer.MAX_VALUE))
      .thenReturn(new Institutions()
        .institutions(Collections.emptyList())
        .totalRecords(0));

    var map = referenceDataService.getInstitutions();

    assertThat(map).isEmpty();
  }

  @Test
  void testGetEmptyLibraries() {
    when(locationUnitsClient.getLibraries(Integer.MAX_VALUE))
      .thenReturn(new Libraries()
        .libraries(Collections.emptyList())
        .totalRecords(0));

    var map = referenceDataService.getLibraries();

    assertThat(map).isEmpty();
  }

  @Test
  void testGetEmptyMaterialTypes() {
    when(materialTypesClient.getMaterialTypes(Integer.MAX_VALUE))
      .thenReturn(new MaterialTypes()
        .mtypes(Collections.emptyList())
        .totalRecords(0));

    var map = referenceDataService.getMaterialTypes();

    assertThat(map).isEmpty();
  }

  @Test
  void testGetEmptyNatureOfContentTerms() {
    when(natureOfContentTermsClient.getNatureOfContentTerms(Integer.MAX_VALUE))
      .thenReturn(new NatureOfContentTerms()
        .natureOfContentTerms(Collections.emptyList())
        .totalRecords(0));

    var map = referenceDataService.getNatureOfContentTerms();

    assertThat(map).isEmpty();
  }

  @Test
  void testGetEmptyIssuanceModes() {
    when(issuanceModesClient.getIssuanceModes(Integer.MAX_VALUE))
      .thenReturn(new IssuanceModes()
        .issuanceModes(Collections.emptyList())
        .totalRecords(0));

    var map = referenceDataService.getIssuanceModes();

    assertThat(map).isEmpty();
  }
}
