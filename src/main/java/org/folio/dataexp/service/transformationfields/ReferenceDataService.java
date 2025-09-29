package org.folio.dataexp.service.transformationfields;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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
import org.folio.dataexp.domain.dto.Alternativetitletype;
import org.folio.dataexp.domain.dto.CallNumberType;
import org.folio.dataexp.domain.dto.Campus;
import org.folio.dataexp.domain.dto.ContributorNameType;
import org.folio.dataexp.domain.dto.ElectronicAccessRelationship;
import org.folio.dataexp.domain.dto.HoldingsNoteType;
import org.folio.dataexp.domain.dto.IdentifierType;
import org.folio.dataexp.domain.dto.InstanceFormat;
import org.folio.dataexp.domain.dto.InstanceType;
import org.folio.dataexp.domain.dto.Institution;
import org.folio.dataexp.domain.dto.ItemNoteType;
import org.folio.dataexp.domain.dto.Library;
import org.folio.dataexp.domain.dto.LoanType;
import org.folio.dataexp.domain.dto.Location;
import org.folio.dataexp.domain.dto.MaterialType;
import org.folio.dataexp.domain.dto.ModeOfIssuance;
import org.folio.dataexp.domain.dto.NatureOfContentTerm;
import org.folio.processor.referencedata.JsonObjectWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

/**
 * Service for retrieving reference data from various clients.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class ReferenceDataService {
  private static final int REFERENCE_DATA_LIMIT = Integer.MAX_VALUE;

  private final ObjectMapper objectMapper;
  private final AlternativeTitleTypesClient alternativeTitleTypesClient;
  private final CallNumberTypesClient callNumberTypesClient;
  private final ContributorNameTypesClient contributorNameTypesClient;
  private final ElectronicAccessRelationshipsClient electronicAccessRelationshipsClient;
  private final HoldingsNoteTypesClient holdingsNoteTypesClient;
  private final IdentifierTypesClient identifierTypesClient;
  private final InstanceFormatsClient instanceFormatsClient;
  private final InstanceTypesClient instanceTypesClient;
  private final ItemNoteTypesClient itemNoteTypesClient;
  private final LoanTypesClient loanTypesClient;
  private final LocationsClient locationsClient;
  private final LocationUnitsClient locationUnitsClient;
  private final MaterialTypesClient materialTypesClient;
  private final NatureOfContentTermsClient natureOfContentTermsClient;
  private final IssuanceModesClient issuanceModesClient;

  /**
   * Gets alternative title types as reference data.
   *
   * @return map of ID to JsonObjectWrapper
   */
  public Map<String, JsonObjectWrapper> getAlternativeTitleTypes() {
    var list = alternativeTitleTypesClient.getAlternativeTitleTypes(REFERENCE_DATA_LIMIT)
        .getAlternativeTitleTypes();
    return ObjectUtils.isEmpty(list)
        ? Collections.emptyMap()
        : list.stream().collect(
            Collectors.toMap(Alternativetitletype::getId, this::toJsonObjectWrapper)
        );
  }

  /**
   * Gets call number types as reference data.
   *
   * @return map of ID to JsonObjectWrapper
   */
  public Map<String, JsonObjectWrapper> getCallNumberTypes() {
    var list = callNumberTypesClient.getCallNumberTypes(REFERENCE_DATA_LIMIT)
        .getCallNumberTypes();
    return ObjectUtils.isEmpty(list)
        ? Collections.emptyMap()
        : list.stream().collect(
            Collectors.toMap(CallNumberType::getId, this::toJsonObjectWrapper)
        );
  }

  /**
   * Gets contributor name types as reference data.
   *
   * @return map of ID to JsonObjectWrapper
   */
  public Map<String, JsonObjectWrapper> getContributorNameTypes() {
    var list = contributorNameTypesClient.getContributorNameTypes(REFERENCE_DATA_LIMIT)
        .getContributorNameTypes();
    return ObjectUtils.isEmpty(list)
        ? Collections.emptyMap()
        : list.stream().collect(
            Collectors.toMap(ContributorNameType::getId, this::toJsonObjectWrapper)
        );
  }

  /**
   * Gets electronic access relationships as reference data.
   *
   * @return map of ID to JsonObjectWrapper
   */
  public Map<String, JsonObjectWrapper> getElectronicAccessRelationships() {
    var list = electronicAccessRelationshipsClient
        .getElectronicAccessRelationships(REFERENCE_DATA_LIMIT)
        .getElectronicAccessRelationships();
    return ObjectUtils.isEmpty(list)
        ? Collections.emptyMap()
        : list.stream().collect(
            Collectors.toMap(ElectronicAccessRelationship::getId, this::toJsonObjectWrapper)
        );
  }

  /**
   * Gets holdings note types as reference data.
   *
   * @return map of ID to JsonObjectWrapper
   */
  public Map<String, JsonObjectWrapper> getHoldingsNoteTypes() {
    var list = holdingsNoteTypesClient.getHoldingsNoteTypes(REFERENCE_DATA_LIMIT)
        .getHoldingsNoteTypes();
    return ObjectUtils.isEmpty(list)
        ? Collections.emptyMap()
        : list.stream().collect(
            Collectors.toMap(HoldingsNoteType::getId, this::toJsonObjectWrapper)
        );
  }

  /**
   * Gets identifier types as reference data.
   *
   * @return map of ID to JsonObjectWrapper
   */
  public Map<String, JsonObjectWrapper> getIdentifierTypes() {
    var list = identifierTypesClient.getIdentifierTypes(REFERENCE_DATA_LIMIT)
        .getIdentifierTypes();
    return ObjectUtils.isEmpty(list)
        ? Collections.emptyMap()
        : list.stream().collect(
            Collectors.toMap(IdentifierType::getId, this::toJsonObjectWrapper)
        );
  }

  /**
   * Gets instance formats as reference data.
   *
   * @return map of ID to JsonObjectWrapper
   */
  public Map<String, JsonObjectWrapper> getInstanceFormats() {
    var list = instanceFormatsClient.getInstanceFormats(REFERENCE_DATA_LIMIT)
        .getInstanceFormats();
    return ObjectUtils.isEmpty(list)
        ? Collections.emptyMap()
        : list.stream().collect(
            Collectors.toMap(InstanceFormat::getId, this::toJsonObjectWrapper)
        );
  }

  /**
   * Gets instance types as reference data.
   *
   * @return map of ID to JsonObjectWrapper
   */
  public Map<String, JsonObjectWrapper> getInstanceTypes() {
    var list = instanceTypesClient.getInstanceTypes(REFERENCE_DATA_LIMIT)
        .getInstanceTypes();
    return ObjectUtils.isEmpty(list)
        ? Collections.emptyMap()
        : list.stream().collect(
            Collectors.toMap(InstanceType::getId, this::toJsonObjectWrapper)
        );
  }

  /**
   * Gets item note types as reference data.
   *
   * @return map of ID to JsonObjectWrapper
   */
  public Map<String, JsonObjectWrapper> getItemNoteTypes() {
    var list = itemNoteTypesClient.getItemNoteTypes(REFERENCE_DATA_LIMIT)
        .getItemNoteTypes();
    return ObjectUtils.isEmpty(list)
        ? Collections.emptyMap()
        : list.stream().collect(
            Collectors.toMap(ItemNoteType::getId, this::toJsonObjectWrapper)
        );
  }

  /**
   * Gets loan types as reference data.
   *
   * @return map of ID to JsonObjectWrapper
   */
  public Map<String, JsonObjectWrapper> getLoanTypes() {
    var list = loanTypesClient.getLoanTypes(REFERENCE_DATA_LIMIT)
        .getLoantypes();
    return ObjectUtils.isEmpty(list)
        ? Collections.emptyMap()
        : list.stream().collect(
            Collectors.toMap(LoanType::getId, this::toJsonObjectWrapper)
        );
  }

  /**
   * Gets locations as reference data.
   *
   * @return map of ID to JsonObjectWrapper
   */
  public Map<String, JsonObjectWrapper> getLocations() {
    var list = locationsClient.getLocations(REFERENCE_DATA_LIMIT)
        .getLocations();
    log.info("getLocations list size: {}", list.size());
    return ObjectUtils.isEmpty(list)
        ? Collections.emptyMap()
        : list.stream().collect(
            Collectors.toMap(Location::getId, this::toJsonObjectWrapper)
        );
  }

  /**
   * Gets campuses as reference data.
   *
   * @return map of ID to JsonObjectWrapper
   */
  public Map<String, JsonObjectWrapper> getCampuses() {
    var list = locationUnitsClient.getCampuses(REFERENCE_DATA_LIMIT)
        .getLoccamps();
    return ObjectUtils.isEmpty(list)
        ? Collections.emptyMap()
        : list.stream().collect(
            Collectors.toMap(Campus::getId, this::toJsonObjectWrapper)
        );
  }

  /**
   * Gets institutions as reference data.
   *
   * @return map of ID to JsonObjectWrapper
   */
  public Map<String, JsonObjectWrapper> getInstitutions() {
    var list = locationUnitsClient.getInstitutions(REFERENCE_DATA_LIMIT)
        .getLocinsts();
    return ObjectUtils.isEmpty(list)
        ? Collections.emptyMap()
        : list.stream().collect(
            Collectors.toMap(Institution::getId, this::toJsonObjectWrapper)
        );
  }

  /**
   * Gets libraries as reference data.
   *
   * @return map of ID to JsonObjectWrapper
   */
  public Map<String, JsonObjectWrapper> getLibraries() {
    var list = locationUnitsClient.getLibraries(REFERENCE_DATA_LIMIT)
        .getLoclibs();
    return ObjectUtils.isEmpty(list)
        ? Collections.emptyMap()
        : list.stream().collect(
            Collectors.toMap(Library::getId, this::toJsonObjectWrapper)
        );
  }

  /**
   * Gets material types as reference data.
   *
   * @return map of ID to JsonObjectWrapper
   */
  public Map<String, JsonObjectWrapper> getMaterialTypes() {
    var list = materialTypesClient.getMaterialTypes(REFERENCE_DATA_LIMIT)
        .getMtypes();
    return ObjectUtils.isEmpty(list)
        ? Collections.emptyMap()
        : list.stream().collect(
            Collectors.toMap(MaterialType::getId, this::toJsonObjectWrapper)
        );
  }

  /**
   * Gets nature of content terms as reference data.
   *
   * @return map of ID to JsonObjectWrapper
   */
  public Map<String, JsonObjectWrapper> getNatureOfContentTerms() {
    var list = natureOfContentTermsClient.getNatureOfContentTerms(REFERENCE_DATA_LIMIT)
        .getNatureOfContentTerms();
    return ObjectUtils.isEmpty(list)
        ? Collections.emptyMap()
        : list.stream().collect(
            Collectors.toMap(NatureOfContentTerm::getId, this::toJsonObjectWrapper)
        );
  }

  /**
   * Gets issuance modes as reference data.
   *
   * @return map of ID to JsonObjectWrapper
   */
  public Map<String, JsonObjectWrapper> getIssuanceModes() {
    var list = issuanceModesClient.getIssuanceModes(REFERENCE_DATA_LIMIT)
        .getIssuanceModes();
    return ObjectUtils.isEmpty(list)
        ? Collections.emptyMap()
        : list.stream().collect(
            Collectors.toMap(ModeOfIssuance::getId, this::toJsonObjectWrapper)
        );
  }

  /**
   * Converts an object to a JsonObjectWrapper.
   *
   * @param o the object
   * @return JsonObjectWrapper
   */
  private JsonObjectWrapper toJsonObjectWrapper(Object o) {
    return new JsonObjectWrapper(
        objectMapper.convertValue(o, new TypeReference<>() {})
    );
  }
}
