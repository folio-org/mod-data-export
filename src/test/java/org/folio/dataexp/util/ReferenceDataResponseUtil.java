package org.folio.dataexp.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class ReferenceDataResponseUtil {
  private static ObjectMapper mapper = new ObjectMapper();

  public static Map<String, JsonObjectWrapper> getIdentifierTypes() throws IOException {
    var collection = mapper.readValue(ReferenceDataResponseUtil.class.getResourceAsStream("/mockdata/inventory/get_identifier_types_response.json"),
      IdentifierTypes.class);
    return collection.getIdentifierTypes().stream()
      .collect(Collectors.toMap(IdentifierType::getId, ReferenceDataResponseUtil::toJsonObjectWrapper));
  }

  public static Map<String, JsonObjectWrapper> getContributorNameTypes() throws IOException {
    var collection = mapper.readValue(ReferenceDataResponseUtil.class.getResourceAsStream("/mockdata/inventory/get_contributor_name_types_response.json"),
      ContributorNameTypes.class);
    return collection.getContributorNameTypes().stream()
      .collect(Collectors.toMap(ContributorNameType::getId, ReferenceDataResponseUtil::toJsonObjectWrapper));
  }

  public static Map<String, JsonObjectWrapper> getMaterialTypes() throws IOException {
    var collection = mapper.readValue(ReferenceDataResponseUtil.class.getResourceAsStream("/mockdata/inventory/get_material_types_response.json"),
      MaterialTypes.class);
    return collection.getMtypes().stream()
      .collect(Collectors.toMap(MaterialType::getId, ReferenceDataResponseUtil::toJsonObjectWrapper));
  }

  public static Map<String, JsonObjectWrapper> getInstanceTypes() throws IOException {
    var collection = mapper.readValue(ReferenceDataResponseUtil.class.getResourceAsStream("/mockdata/inventory/get_instance_types_response.json"),
      InstanceTypes.class);
    return collection.getInstanceTypes().stream()
      .collect(Collectors.toMap(InstanceType::getId, ReferenceDataResponseUtil::toJsonObjectWrapper));
  }

  public static Map<String, JsonObjectWrapper> getElectronicAccessRelationships() throws IOException {
    var collection = mapper.readValue(ReferenceDataResponseUtil.class.getResourceAsStream("/mockdata/inventory/get_electronic_access_relationships_response.json"),
      ElectronicAccessRelationships.class);
    return collection.getElectronicAccessRelationships().stream()
      .collect(Collectors.toMap(ElectronicAccessRelationship::getId, ReferenceDataResponseUtil::toJsonObjectWrapper));
  }

  public static Map<String, JsonObjectWrapper> getAlternativeTitleTypes() throws IOException {
    var collection = mapper.readValue(ReferenceDataResponseUtil.class.getResourceAsStream("/mockdata/inventory/get_alternative_titles_response.json"),
      Alternativetitletypes.class);
    return collection.getAlternativeTitleTypes().stream()
      .collect(Collectors.toMap(Alternativetitletype::getId, ReferenceDataResponseUtil::toJsonObjectWrapper));
  }

  public static Map<String, JsonObjectWrapper> getModeOfIssuance() throws IOException {
    var collection = mapper.readValue(ReferenceDataResponseUtil.class.getResourceAsStream("/mockdata/inventory/get_mode_of_issuance_response.json"),
      IssuanceModes.class);
    return collection.getIssuanceModes().stream()
      .collect(Collectors.toMap(ModeOfIssuance::getId, ReferenceDataResponseUtil::toJsonObjectWrapper));
  }

  public static Map<String, JsonObjectWrapper> getLoanTypes() throws IOException {
    var collection = mapper.readValue(ReferenceDataResponseUtil.class.getResourceAsStream("/mockdata/inventory/get_loan_types_response.json"),
      LoanTypes.class);
    return collection.getLoantypes().stream()
      .collect(Collectors.toMap(LoanType::getId, ReferenceDataResponseUtil::toJsonObjectWrapper));
  }

  public static Map<String, JsonObjectWrapper> getHoldingNoteTypes() throws IOException {
    var collection = mapper.readValue(ReferenceDataResponseUtil.class.getResourceAsStream("/mockdata/inventory/get_holding_note_types_response.json"),
      HoldingsNoteTypes.class);
    return collection.getHoldingsNoteTypes().stream()
      .collect(Collectors.toMap(HoldingsNoteType::getId, ReferenceDataResponseUtil::toJsonObjectWrapper));
  }

  public static Map<String, JsonObjectWrapper> getItemNoteTypes() throws IOException {
    var collection = mapper.readValue(ReferenceDataResponseUtil.class.getResourceAsStream("/mockdata/inventory/get_item_note_types_response.json"),
      ItemNoteTypes.class);
    return collection.getItemNoteTypes().stream()
      .collect(Collectors.toMap(ItemNoteType::getId, ReferenceDataResponseUtil::toJsonObjectWrapper));
  }

  private static JsonObjectWrapper toJsonObjectWrapper(Object o) {
    return new JsonObjectWrapper(mapper.convertValue(o, new TypeReference<>() {}));
  }
}