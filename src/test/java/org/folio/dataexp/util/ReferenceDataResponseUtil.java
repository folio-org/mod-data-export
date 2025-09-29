package org.folio.dataexp.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import org.folio.dataexp.domain.dto.Alternativetitletype;
import org.folio.dataexp.domain.dto.Alternativetitletypes;
import org.folio.dataexp.domain.dto.ContributorNameType;
import org.folio.dataexp.domain.dto.ContributorNameTypes;
import org.folio.dataexp.domain.dto.ElectronicAccessRelationship;
import org.folio.dataexp.domain.dto.ElectronicAccessRelationships;
import org.folio.dataexp.domain.dto.HoldingsNoteType;
import org.folio.dataexp.domain.dto.HoldingsNoteTypes;
import org.folio.dataexp.domain.dto.IdentifierType;
import org.folio.dataexp.domain.dto.IdentifierTypes;
import org.folio.dataexp.domain.dto.InstanceType;
import org.folio.dataexp.domain.dto.InstanceTypes;
import org.folio.dataexp.domain.dto.IssuanceModes;
import org.folio.dataexp.domain.dto.ItemNoteType;
import org.folio.dataexp.domain.dto.ItemNoteTypes;
import org.folio.dataexp.domain.dto.LoanType;
import org.folio.dataexp.domain.dto.LoanTypes;
import org.folio.dataexp.domain.dto.MaterialType;
import org.folio.dataexp.domain.dto.MaterialTypes;
import org.folio.dataexp.domain.dto.ModeOfIssuance;
import org.folio.processor.referencedata.JsonObjectWrapper;

/**
 * Utility class for loading and converting reference data responses from JSON files
 * into maps of {@link JsonObjectWrapper} keyed by their IDs.
 *
 * <p>This class provides static methods to load various types of reference data used in tests,
 * such as identifier types, contributor name types, material types, instance types, and more.
 * Each method reads a corresponding mock JSON file and converts the data into a map for use
 * in tests.
 * </p>
 */
public class ReferenceDataResponseUtil {
  private static ObjectMapper mapper = new ObjectMapper();

  /**
   * Loads identifier types from the mock JSON file and returns them as a map.
   *
   * @return map of identifier type ID to {@link JsonObjectWrapper}
   * @throws IOException if the file cannot be read
   */
  public static Map<String, JsonObjectWrapper> getIdentifierTypes() throws IOException {
    var collection = mapper.readValue(ReferenceDataResponseUtil.class.getResourceAsStream(
        "/mockdata/inventory/get_identifier_types_response.json"),
        IdentifierTypes.class);
    return collection.getIdentifierTypes().stream()
        .collect(Collectors.toMap(IdentifierType::getId,
            ReferenceDataResponseUtil::toJsonObjectWrapper));
  }

  /**
   * Loads contributor name types from the mock JSON file and returns them as a map.
   *
   * @return map of contributor name type ID to {@link JsonObjectWrapper}
   * @throws IOException if the file cannot be read
   */
  public static Map<String, JsonObjectWrapper> getContributorNameTypes() throws IOException {
    var collection = mapper.readValue(ReferenceDataResponseUtil.class.getResourceAsStream(
        "/mockdata/inventory/get_contributor_name_types_response.json"),
        ContributorNameTypes.class);
    return collection.getContributorNameTypes().stream()
        .collect(Collectors.toMap(ContributorNameType::getId,
            ReferenceDataResponseUtil::toJsonObjectWrapper));
  }

  /**
   * Loads material types from the mock JSON file and returns them as a map.
   *
   * @return map of material type ID to {@link JsonObjectWrapper}
   * @throws IOException if the file cannot be read
   */
  public static Map<String, JsonObjectWrapper> getMaterialTypes() throws IOException {
    var collection = mapper.readValue(ReferenceDataResponseUtil.class.getResourceAsStream(
        "/mockdata/inventory/get_material_types_response.json"), MaterialTypes.class);
    return collection.getMtypes().stream()
        .collect(Collectors.toMap(MaterialType::getId,
            ReferenceDataResponseUtil::toJsonObjectWrapper));
  }

  /**
   * Loads instance types from the mock JSON file and returns them as a map.
   *
   * @return map of instance type ID to {@link JsonObjectWrapper}
   * @throws IOException if the file cannot be read
   */
  public static Map<String, JsonObjectWrapper> getInstanceTypes() throws IOException {
    var collection = mapper.readValue(ReferenceDataResponseUtil.class.getResourceAsStream(
        "/mockdata/inventory/get_instance_types_response.json"), InstanceTypes.class);
    return collection.getInstanceTypes().stream()
      .collect(Collectors.toMap(InstanceType::getId,
          ReferenceDataResponseUtil::toJsonObjectWrapper));
  }

  /**
   * Loads electronic access relationships from the mock JSON file and returns them as a map.
   *
   * @return map of electronic access relationship ID to {@link JsonObjectWrapper}
   * @throws IOException if the file cannot be read
   */
  public static Map<String, JsonObjectWrapper> getElectronicAccessRelationships()
      throws IOException {
    var collection = mapper.readValue(ReferenceDataResponseUtil.class.getResourceAsStream(
        "/mockdata/inventory/get_electronic_access_relationships_response.json"),
        ElectronicAccessRelationships.class);
    return collection.getElectronicAccessRelationships().stream()
        .collect(Collectors.toMap(ElectronicAccessRelationship::getId,
            ReferenceDataResponseUtil::toJsonObjectWrapper));
  }

  /**
   * Loads alternative title types from the mock JSON file and returns them as a map.
   *
   * @return map of alternative title type ID to {@link JsonObjectWrapper}
   * @throws IOException if the file cannot be read
   */
  public static Map<String, JsonObjectWrapper> getAlternativeTitleTypes() throws IOException {
    var collection = mapper.readValue(ReferenceDataResponseUtil.class.getResourceAsStream(
        "/mockdata/inventory/get_alternative_titles_response.json"),
        Alternativetitletypes.class);
    return collection.getAlternativeTitleTypes().stream()
        .collect(Collectors.toMap(Alternativetitletype::getId,
            ReferenceDataResponseUtil::toJsonObjectWrapper));
  }

  /**
   * Loads modes of issuance from the mock JSON file and returns them as a map.
   *
   * @return map of mode of issuance ID to {@link JsonObjectWrapper}
   * @throws IOException if the file cannot be read
   */
  public static Map<String, JsonObjectWrapper> getModeOfIssuance() throws IOException {
    var collection = mapper.readValue(ReferenceDataResponseUtil.class.getResourceAsStream(
        "/mockdata/inventory/get_mode_of_issuance_response.json"), IssuanceModes.class);
    return collection.getIssuanceModes().stream()
        .collect(Collectors.toMap(ModeOfIssuance::getId,
            ReferenceDataResponseUtil::toJsonObjectWrapper));
  }

  /**
   * Loads loan types from the mock JSON file and returns them as a map.
   *
   * @return map of loan type ID to {@link JsonObjectWrapper}
   * @throws IOException if the file cannot be read
   */
  public static Map<String, JsonObjectWrapper> getLoanTypes() throws IOException {
    var collection = mapper.readValue(ReferenceDataResponseUtil.class.getResourceAsStream(
        "/mockdata/inventory/get_loan_types_response.json"), LoanTypes.class);
    return collection.getLoantypes().stream()
        .collect(Collectors.toMap(LoanType::getId,
            ReferenceDataResponseUtil::toJsonObjectWrapper));
  }

  /**
   * Loads holding note types from the mock JSON file and returns them as a map.
   *
   * @return map of holding note type ID to {@link JsonObjectWrapper}
   * @throws IOException if the file cannot be read
   */
  public static Map<String, JsonObjectWrapper> getHoldingNoteTypes() throws IOException {
    var collection = mapper.readValue(ReferenceDataResponseUtil.class.getResourceAsStream(
        "/mockdata/inventory/get_holding_note_types_response.json"), HoldingsNoteTypes.class);
    return collection.getHoldingsNoteTypes().stream()
        .collect(Collectors.toMap(HoldingsNoteType::getId,
            ReferenceDataResponseUtil::toJsonObjectWrapper));
  }

  /**
   * Loads item note types from the mock JSON file and returns them as a map.
   *
   * @return map of item note type ID to {@link JsonObjectWrapper}
   * @throws IOException if the file cannot be read
   */
  public static Map<String, JsonObjectWrapper> getItemNoteTypes() throws IOException {
    var collection = mapper.readValue(ReferenceDataResponseUtil.class.getResourceAsStream(
        "/mockdata/inventory/get_item_note_types_response.json"), ItemNoteTypes.class);
    return collection.getItemNoteTypes().stream()
        .collect(Collectors.toMap(ItemNoteType::getId,
            ReferenceDataResponseUtil::toJsonObjectWrapper));
  }

  /**
   * Converts the given object to a {@link JsonObjectWrapper}.
   *
   * @param o the object to convert
   * @return the wrapped JSON object
   */
  private static JsonObjectWrapper toJsonObjectWrapper(Object o) {
    return new JsonObjectWrapper(mapper.convertValue(o, new TypeReference<>() {}));
  }
}
