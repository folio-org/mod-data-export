package org.folio.service.mapping;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.folio.TestUtil;
import org.folio.processor.ReferenceData;
import org.folio.processor.rule.Metadata;
import org.folio.processor.translations.Translation;
import org.folio.processor.translations.TranslationFunction;
import org.folio.service.mapping.referencedata.ReferenceDataImpl;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.CONTRIBUTOR_NAME_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.ELECTRONIC_ACCESS_RELATIONSHIPS;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.IDENTIFIER_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.INSTANCE_FORMATS;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.INSTANCE_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.LOCATIONS;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.MATERIAL_TYPES;
import static org.folio.service.mapping.referencedata.ReferenceDataImpl.NATURE_OF_CONTENT_TERMS;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RunWith(MockitoJUnitRunner.class)
class TranslationFunctionHolderUnitTest {
  private static ReferenceData referenceData = new ReferenceDataImpl();

  @BeforeAll
  static void setUp() {
    referenceData.put(NATURE_OF_CONTENT_TERMS, getNatureOfContentTerms());
    referenceData.put(IDENTIFIER_TYPES, getIdentifierTypes());
    referenceData.put(CONTRIBUTOR_NAME_TYPES, getContributorNameTypes());
    referenceData.put(LOCATIONS, getLocations());
    referenceData.put(MATERIAL_TYPES, getMaterialTypes());
    referenceData.put(INSTANCE_TYPES, getInstanceTypes());
    referenceData.put(INSTANCE_FORMATS, getInstanceFormats());
    referenceData.put(ELECTRONIC_ACCESS_RELATIONSHIPS, getElectronicAccessRelationships());
  }

  private static Map<String, JsonObject> getNatureOfContentTerms() {
    JsonObject natureOfContentTerm =
      new JsonObject(TestUtil.readFileContentFromResources("mockData/inventory/get_nature_of_content_terms_response.json"))
        .getJsonArray("natureOfContentTerms")
        .getJsonObject(0);
    return Collections.singletonMap(natureOfContentTerm.getString("id"), natureOfContentTerm);
  }

  private static Map<String, JsonObject> getIdentifierTypes() {
    JsonObject identifierType =
      new JsonObject(TestUtil.readFileContentFromResources("mockData/inventory/get_identifier_types_response.json"))
        .getJsonArray("identifierTypes")
        .getJsonObject(0);
    return Collections.singletonMap(identifierType.getString("id"), identifierType);
  }

  private static Map<String, JsonObject> getContributorNameTypes() {
    JsonObject contributorNameTypes =
      new JsonObject(TestUtil.readFileContentFromResources("mockData/inventory/get_contributor_name_types_response.json"))
        .getJsonArray("contributorNameTypes")
        .getJsonObject(0);
    return Collections.singletonMap(contributorNameTypes.getString("id"), contributorNameTypes);
  }

  private static Map<String, JsonObject> getLocations() {
    JsonObject identifierType =
      new JsonObject(TestUtil.readFileContentFromResources("mockData/inventory/get_locations_response.json"))
        .getJsonArray("locations")
        .getJsonObject(0);
    return Collections.singletonMap(identifierType.getString("id"), identifierType);
  }

  private static Map<String, JsonObject> getMaterialTypes() {
    JsonObject identifierType =
      new JsonObject(TestUtil.readFileContentFromResources("mockData/inventory/get_material_types_response.json"))
        .getJsonArray("mtypes")
        .getJsonObject(0);
    return Collections.singletonMap(identifierType.getString("id"), identifierType);
  }

  private static Map<String, JsonObject> getInstanceTypes() {
    JsonObject instanceTypes =
      new JsonObject(TestUtil.readFileContentFromResources("mockData/inventory/get_instance_types_response.json"))
        .getJsonArray("instanceTypes")
        .getJsonObject(0);
    return Collections.singletonMap(instanceTypes.getString("id"), instanceTypes);
  }

  private static Map<String, JsonObject> getInstanceFormats() {
    Map<String, JsonObject> stringJsonObjectMap = new HashMap<>();
    JsonArray instanceFormats =
      new JsonObject(TestUtil.readFileContentFromResources("mockData/inventory/get_instance_formats_response.json"))
        .getJsonArray("instanceFormats");
    instanceFormats.stream().forEach(instanceFormat -> {
      JsonObject jsonObject = new JsonObject(instanceFormat.toString());
      stringJsonObjectMap.put(jsonObject.getString("id"), jsonObject);
    });
    return stringJsonObjectMap;
  }

  private static Map<String, JsonObject> getElectronicAccessRelationships() {
    Map<String, JsonObject> stringJsonObjectMap = new HashMap<>();
    JsonArray electronicAccessRelationships =
      new JsonObject(TestUtil.readFileContentFromResources("mockData/inventory/get_electronic_access_relationships_response.json"))
        .getJsonArray("electronicAccessRelationships");
    electronicAccessRelationships.stream().forEach(electronicAccessRelationship -> {
      JsonObject jsonObject = new JsonObject(electronicAccessRelationship.toString());
      stringJsonObjectMap.put(jsonObject.getString("id"), jsonObject);
    });
    return stringJsonObjectMap;
  }

  @Test
  void SetValue_shouldSetGivenValue() {
    // given
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_value");
    String value = "field value";
    Translation translation = new Translation();
    translation.setParameters(Collections.singletonMap("value", value));
    // when
    String result = translationFunction.apply(value, 0, translation, null, null);
    // then
    Assert.assertEquals(value, result);
  }

  @Test
  void SetNatureOfContentTerm_shouldReturnTermName() {
    // given
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_nature_of_content_term");
    String value = "44cd89f3-2e76-469f-a955-cc57cb9e0395";
    // when
    String result = translationFunction.apply(value, 0, null, referenceData, null);
    // then
    Assert.assertEquals("textbook", result);
  }

  @Test
  void SetNatureOfContentTerm_shouldReturnEmptyString() {
    // given
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_nature_of_content_term");
    String value = "non-existing-id";
    // when
    String result = translationFunction.apply(value, 0, null, referenceData, null);
    // then
    Assert.assertEquals(StringUtils.EMPTY, result);
  }

  @Test
  void SetIdentifier_shouldReturnIdentifierValue() {
    // given
    String value = "value";
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_identifier");

    Translation translation = new Translation();
    translation.setParameters(Collections.singletonMap("type", "LCCN"));

    Metadata metadata = new Metadata();
    metadata.addData("identifierTypeId",
      new Metadata.Entry("$.identifiers[*].identifierTypeId",
        asList("8261054f-be78-422d-bd51-4ed9f33c3422", "c858e4f2-2b6b-4385-842b-60732ee14abb")));
    // when
    String result = translationFunction.apply(value, 1, translation, referenceData, metadata);
    // then
    Assert.assertEquals(value, result);
  }

  @Test
  void SetIdentifier_shouldReturnEmptyString_whenMetadataIsEmpty() {
    // given
    String value = "value";
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_identifier");

    Translation translation = new Translation();
    translation.setParameters(Collections.singletonMap("type", "LCCN"));

    Metadata metadata = new Metadata();
    metadata.addData("identifierTypeId", new Metadata.Entry("$.identifiers[*].identifierTypeId", Collections.emptyList()));
    // when
    String result = translationFunction.apply(value, 0, translation, referenceData, metadata);
    // then
    Assert.assertEquals(StringUtils.EMPTY, result);
  }


  @Test
  void SetMaterialType_shouldReturnMaterialTypeValue() {
    // given
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_material_type");
    String value = "1a54b431-2e4f-452d-9cae-9cee66c9a892";
    // when
    String result = translationFunction.apply(value, 0, null, referenceData, null);
    // then
    Assert.assertEquals("book", result);
  }

  @Test
  void SetMaterialType_shouldReturnEmptyString() {
    // given
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_material_type");
    String value = "non-existing-id";
    // when
    String result = translationFunction.apply(value, 0, null, referenceData, null);
    // then
    Assert.assertEquals(StringUtils.EMPTY, result);
  }

  @Test
  void SetInstanceTypeId_shouldReturnInstanceTypeIdValue() {
    // given
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_instance_type_id");
    String value = "6312d172-f0cf-40f6-b27d-9fa8feaf332f";
    // when
    String result = translationFunction.apply(value, 0, null, referenceData, null);
    // then
    Assert.assertEquals("text", result);
  }

  @Test
  void SetInstanceTypeId_shouldReturnEmptyString() {
    // given
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_instance_type_id");
    String value = "non-existing-id";
    // when
    String result = translationFunction.apply(value, 0, null, referenceData, null);
    // then
    Assert.assertEquals(StringUtils.EMPTY, result);
  }

  @Test
  void SetInstanceFormatId_shouldReturnInstanceFormatIdValue() {
    // given
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_instance_format_id");
    Translation translation = new Translation();
    translation.setParameters(Collections.singletonMap("value", "0"));
    String value = "7fde4e21-00b5-4de4-a90a-08a84a601aeb";
    // when
    String result = translationFunction.apply(value, 0, translation, referenceData, null);
    // then
    Assert.assertEquals("audio", result);
  }

  @Test
  void SetInstanceFormatId_shouldReturnInstanceFormatIdValue_IfNoRegexFromInventory() {
    // given
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_instance_format_id");
    Translation translation = new Translation();
    translation.setParameters(Collections.singletonMap("value", "0"));
    String value = "485e3e1d-9f46-42b6-8c65-6bb7bd4b37f8";
    // when
    String result = translationFunction.apply(value, 0, translation, referenceData, null);
    // then
    Assert.assertEquals("microform", result);
  }

  @Test
  void SetInstanceFormatId_shouldReturnEmptyString_IfNoRegexFromInventory() {
    // given
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_instance_format_id");
    Translation translation = new Translation();
    translation.setParameters(Collections.singletonMap("value", "1"));
    String value = "485e3e1d-9f46-42b6-8c65-6bb7bd4b37f8";
    // when
    String result = translationFunction.apply(value, 0, translation, referenceData, null);
    // then
    Assert.assertEquals(StringUtils.EMPTY, result);
  }

  @Test
  void SetInstanceFormatId_shouldReturnEmptyString() {
    // given
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_instance_format_id");
    String value = "non-existing-id";
    // when
    String result = translationFunction.apply(value, 0, null, referenceData, null);
    // then
    Assert.assertEquals(StringUtils.EMPTY, result);
  }

  @Test
  void SetTransactionDatetime_shouldReturnFormattedDate() {
    // given
    String updatedDate = "2020-05-22T01:46:42.915+0000";
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_transaction_datetime");
    // when
    String result = translationFunction.apply(updatedDate, 0, null, null, null);
    // then
    Assert.assertNotNull(result);
    Assert.assertEquals("20200522014642.9", result);
  }

  @Test
  void SetTransactionDatetime_shouldThrowException() {
    // given
    String updatedDate = "date in wrong format";
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_transaction_datetime");
    // when
    assertThrows(DateTimeParseException.class, () ->
      translationFunction.apply(updatedDate, 0, null, null, null)
    );
  }

  @Test
  void SetContributor_shouldReturnContributorNameValue() {
    // given
    String value = "value";
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_contributor");

    Translation translation = new Translation();
    translation.setParameters(Collections.singletonMap("type", "Personal name"));

    Metadata metadata = new Metadata();
    metadata.addData("contributorNameTypeId",
      new Metadata.Entry("$.contributors[?(!(@.primary) || @.primary == false)].contributorNameTypeId",
        Arrays.asList("2b94c631-fca9-4892-a730-03ee529ffe2a", "2e48e713-17f3-4c13-a9f8-23845bb210aa")));
    // when
    String result = translationFunction.apply(value, 0, translation, referenceData, metadata);
    // then
    Assert.assertEquals(value, result);
  }

  @Test
  void setContributor_shouldReturnEmptyString_whenMetadataIsEmpty() {
    // given
    String value = "value";
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_contributor");

    Translation translation = new Translation();
    translation.setParameters(Collections.singletonMap("type", "Personal name"));

    Metadata metadata = new Metadata();
    metadata.addData("contributorNameTypeId", new Metadata.Entry("$.instance.contributors[?(@.primary && @.primary == true)].contributorNameTypeId", Collections.emptyList()));
    // when
    String result = translationFunction.apply(value, 0, translation, referenceData, metadata);
    // then
    Assert.assertEquals(StringUtils.EMPTY, result);
  }


  @Test
  void SetFixedLengthDataElements_noDatesOfPublication_noLanguages_specified() {
    // given
    String createdDate = "2019-08-07T03:12:01.011+0000";
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_fixed_length_data_elements");
    Metadata metadata = new Metadata();
    // when
    String result = translationFunction.apply(createdDate, 0, null, null, metadata);
    // then
    Assert.assertEquals(40, result.length());
    Assert.assertEquals("190807|||||||||||||||||       |||||und||", result);
  }

  @Test
  void SetFixedLengthDataElements_noDatesOfPublication_language_specified() {
    // given
    String createdDate = "2019-08-07T03:12:01.011+0000";
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_fixed_length_data_elements");
    Metadata metadata = new Metadata();
    metadata.addData("languages", new Metadata.Entry("$.languages", singletonList("lat")));
    // when
    String result = translationFunction.apply(createdDate, 0, null, null, metadata);
    // then
    Assert.assertEquals(40, result.length());
    Assert.assertEquals("190807|||||||||||||||||       |||||lat||", result);
  }

  @Test
  void SetFixedLengthDataElements_noDatesOfPublication_multipleLanguages_specified() {
    // given
    String createdDate = "2019-08-07T03:12:01.011+0000";
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_fixed_length_data_elements");
    Metadata metadata = new Metadata();
    metadata.addData("languages", new Metadata.Entry("$.languages", asList("lat", "ita")));
    // when
    String result = translationFunction.apply(createdDate, 0, null, null, metadata);
    // then
    Assert.assertEquals(40, result.length());
    Assert.assertEquals("190807|||||||||||||||||       |||||mul||", result);
  }

  @Test
  void SetFixedLengthDataElements_1dateOfPublication_noLanguages_specified() {
    // given
    String createdDate = "2019-08-07T03:12:01.011+0000";
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_fixed_length_data_elements");
    Metadata metadata = new Metadata();
    metadata.addData("datesOfPublication", new Metadata.Entry("$.publication[*].dateOfPublication", singletonList("2015")));
    // when
    String result = translationFunction.apply(createdDate, 0, null, null, metadata);
    // then
    Assert.assertEquals(40, result.length());
    Assert.assertEquals("190807|2015||||||||||||       |||||und||", result);
  }

  @Test
  void SetFixedLengthDataElements_2datesOfPublication_noLanguages_specified() {
    // given
    String createdDate = "2019-08-07T03:12:01.011+0000";
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_fixed_length_data_elements");
    Metadata metadata = new Metadata();
    metadata.addData("datesOfPublication", new Metadata.Entry("$.publication[*].dateOfPublication", asList("2015", "2016")));
    // when
    String result = translationFunction.apply(createdDate, 0, null, null, metadata);
    // then
    Assert.assertEquals(40, result.length());
    Assert.assertEquals("190807|20152016||||||||       |||||und||", result);
  }

  @Test
  void SetFixedLengthDataElements_2datesOfPublication_multipleLanguages_specified() {
    // given
    String createdDate = "2019-08-07T03:12:01.011+0000";
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_fixed_length_data_elements");
    Metadata metadata = new Metadata();
    metadata.addData("datesOfPublication", new Metadata.Entry("$.publication[*].dateOfPublication", asList("2015", "2016")));
    metadata.addData("languages", new Metadata.Entry("$.languages", asList("lat", "ita")));
    // when
    String result = translationFunction.apply(createdDate, 0, null, null, metadata);
    // then
    Assert.assertEquals(40, result.length());
    Assert.assertEquals("190807|20152016||||||||       |||||mul||", result);
  }

  @Test
  void SetFixedLengthDataElements_2incorrectDatesOfPublication_noLanguages_specified() {
    // given
    String createdDate = "2019-08-07T03:12:01.011+0000";
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_fixed_length_data_elements");
    Metadata metadata = new Metadata();
    metadata.addData("datesOfPublication", new Metadata.Entry("$.publication[*].dateOfPublication", asList("123", "456")));
    // when
    String result = translationFunction.apply(createdDate, 0, null, null, metadata);
    // then
    Assert.assertEquals(40, result.length());
    Assert.assertEquals("190807|||||||||||||||||       |||||und||", result);
  }

  @Test
  void SetFixedLengthDataElements_datesOfPublication_isNull_noLanguages_specified() {
    // given
    String createdDate = "2019-08-07T03:12:01.011+0000";
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_fixed_length_data_elements");
    Metadata metadata = new Metadata();
    metadata.addData("datesOfPublication", null);
    // when
    String result = translationFunction.apply(createdDate, 0, null, null, metadata);
    // then
    Assert.assertEquals(40, result.length());
    Assert.assertEquals("190807|||||||||||||||||       |||||und||", result);
  }

  @Test
  void SetFixedLengthDataElements_datesOfPublication_isNull_languagesIsNull() {
    // given
    String createdDate = "2019-08-07T03:12:01.011+0000";
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_fixed_length_data_elements");
    Metadata metadata = new Metadata();
    metadata.addData("datesOfPublication", null);
    metadata.addData("languages", null);
    // when
    String result = translationFunction.apply(createdDate, 0, null, null, metadata);
    // then
    Assert.assertEquals(40, result.length());
    Assert.assertEquals("190807|||||||||||||||||       |||||und||", result);
  }

  @Test
  void SetFixedLengthDataElements_datesOfPublication_isNull_languagesIsEmpty() {
    // given
    String createdDate = "2019-08-07T03:12:01.011+0000";
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_fixed_length_data_elements");
    Metadata metadata = new Metadata();
    metadata.addData("datesOfPublication", null);
    metadata.addData("languages", new Metadata.Entry("$.languages", singletonList(StringUtils.EMPTY)));
    // when
    String result = translationFunction.apply(createdDate, 0, null, null, metadata);
    // then
    Assert.assertEquals(40, result.length());
    Assert.assertEquals("190807|||||||||||||||||       |||||und||", result);
  }

  @Test
  void SetFixedLengthDataElements_datesOfPublicationFirstParam_isNull_noLanguages_specified() {
    // given
    String createdDate = "2019-08-07T03:12:01.011+0000";
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_fixed_length_data_elements");
    Metadata metadata = new Metadata();
    metadata.addData("datesOfPublication", new Metadata.Entry("$.publication[*].dateOfPublication", asList(null, "2016")));
    // when
    String result = translationFunction.apply(createdDate, 0, null, null, metadata);
    // then
    Assert.assertEquals(40, result.length());
    Assert.assertEquals("190807|||||2016||||||||       |||||und||", result);
  }

  @Test
  void SetFixedLengthDataElements_datesOfPublicationSecondParam_isNull_noLanguages_specified() {
    // given
    String createdDate = "2019-08-07T03:12:01.011+0000";
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_fixed_length_data_elements");
    Metadata metadata = new Metadata();
    metadata.addData("datesOfPublication", new Metadata.Entry("$.publication[*].dateOfPublication", asList("2016", null)));
    // when
    String result = translationFunction.apply(createdDate, 0, null, null, metadata);
    // then
    Assert.assertEquals(40, result.length());
    Assert.assertEquals("190807|2016||||||||||||       |||||und||", result);
  }

  @Test
  void SetFixedLengthDataElements_metadataIsNull() {
    // given
    String createdDate = "2019-08-07T03:12:01.011+0000";
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_fixed_length_data_elements");
    // when
    String result = translationFunction.apply(createdDate, 0, null, null, null);
    // then
    Assert.assertEquals(40, result.length());
    Assert.assertEquals("190807|||||||||||||||||       |||||und||", result);
  }

  @Test
  void SetFixedLengthDataElements_metadataIsNull_createdDateIsNull() {
    // given
    String createdDate = null;
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_fixed_length_data_elements");
    // when
    String result = translationFunction.apply(createdDate, 0, null, null, null);
    // then
    Assert.assertEquals(40, result.length());
  }

  @Test
  void SetFixedLengthDataElements_metadataIsNull_createdDateIsIncorrect() {
    // given
    String createdDate = "date in wrong format";
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_fixed_length_data_elements");
    // when
    String result = translationFunction.apply(createdDate, 0, null, null, null);
    // then
    Assert.assertEquals(40, result.length());
  }

  @Test
  void SetElectronicAccessIndicator_shouldReturnEmptyIndicator_whenRelationshipIdsEmpty() {
    // given
    Metadata metadata = new Metadata();
    metadata.addData("relationshipId", new Metadata.Entry("$.instance.electronicAccess[*].relationshipId", Lists.emptyList()));
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_electronic_access_indicator");
    // when
    String result = translationFunction.apply(null, 0, null, null, metadata);
    // then
    Assert.assertEquals(StringUtils.SPACE, result);
  }

  @Test
  void SetElectronicAccessIndicator_shouldReturnEmptyIndicator_whenRelationshipIdNotExist() {
    // given
    Metadata metadata = new Metadata();
    metadata.addData("relationshipId", new Metadata.Entry("$.instance.electronicAccess[*].relationshipId", Arrays.asList("non-existing-id")));
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_electronic_access_indicator");
    // when
    String result = translationFunction.apply(null, 0, null, referenceData, metadata);
    // then
    Assert.assertEquals(StringUtils.SPACE, result);
  }

  @Test
  void SetElectronicAccessIndicator_shouldReturnEmptyIndicator_whenRelationshipNotEqualTranslationParameterKey() {
    // given
    Metadata metadata = new Metadata();
    metadata.addData("relationshipId", new Metadata.Entry("$.instance.electronicAccess[*].relationshipId", Arrays.asList("f50c90c9-bae0-4add-9cd0-db9092dbc9dd")));
    Map<String, String> parameters = new HashMap<>();
    parameters.put("Resource", "0");
    Translation translation = new Translation();
    translation.setParameters(parameters);
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_electronic_access_indicator");
    // when
    String result = translationFunction.apply(null, 0, translation, referenceData, metadata);
    // then
    Assert.assertEquals(StringUtils.SPACE, result);
  }

  @Test
  void SetElectronicAccessIndicator_shouldReturnParameterIndicator_whenRelationshipEqualsTranslationParameterKey() {
    // given
    Metadata metadata = new Metadata();
    metadata.addData("relationshipId", new Metadata.Entry("$.instance.electronicAccess[*].relationshipId", Arrays.asList("f5d0068e-6272-458e-8a81-b85e7b9a14aa")));
    Map<String, String> parameters = new HashMap<>();
    parameters.put("Resource", "0");
    Translation translation = new Translation();
    translation.setParameters(parameters);
    TranslationFunction translationFunction = TranslationsFunctionHolder.SET_VALUE.lookup("set_electronic_access_indicator");
    // when
    String result = translationFunction.apply(null, 0, translation, referenceData, metadata);
    // then
    Assert.assertEquals("0", result);
  }


}
