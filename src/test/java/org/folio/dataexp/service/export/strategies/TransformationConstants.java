package org.folio.dataexp.service.export.strategies;

public final class TransformationConstants {

  public static final String PERMANENT_LOCATION_FIELD_ID = "holdings.permanentlocation.name";
  public static final String PERMANENT_LOCATION_CODE_FIELD_ID = "holdings.permanentlocation.code";
  public static final String PERMANENT_LOCATION_LIBRARY_NAME_FIELD_ID = "holdings.permanentlocation.library.name";
  public static final String PERMANENT_LOCATION_LIBRARY_CODE_FIELD_ID = "holdings.permanentlocation.library.code";
  public static final String PERMANENT_LOCATION_CAMPUS_NAME_FIELD_ID = "holdings.permanentlocation.campus.name";
  public static final String PERMANENT_LOCATION_CAMPUS_CODE_FIELD_ID = "holdings.permanentlocation.campus.code";
  public static final String PERMANENT_LOCATION_INSTITUTION_NAME_FIELD_ID = "holdings.permanentlocation.institution.name";
  public static final String PERMANENT_LOCATION_INSTITUTION_CODE_FIELD_ID = "holdings.permanentlocation.institution.code";
  public static final String ONE_WORD_LOCATION_FIELD_ID = "permanentlocation";
  public static final String PERMANENT_LOCATION_PATH = "$.holdings[*].permanentLocationId";
  public static final String TEMPORARY_LOCATION_FIELD_ID = "holdings.temporarylocation.name";
  public static final String TEMPORARY_LOCATION_PATH = "$.holdings[*].temporaryLocationId";
  public static final String EFFECTIVE_LOCATION_FIELD_ID = "item.effectivelocation.name";
  public static final String EFFECTIVE_LOCATION_PATH = "$.holdings[*].items[*].effectiveLocationId";
  public static final String SET_LOCATION_FUNCTION = "set_location";
  public static final String MATERIAL_TYPE_FIELD_ID = "item.materialtypeid";
  public static final String MATERIAL_TYPE_PATH = "$.holdings[*].items[*].materialTypeId";
  public static final String SET_MATERIAL_TYPE_FUNCTION = "set_material_type";
  public static final String CALLNUMBER_FIELD_ID = "callNumber";
  public static final String CALLNUMBER_FIELD_PATH = "$.holdings[*].callNumber";
  public static final String CALLNUMBER_PREFIX_FIELD_ID = "callNumberPrefix";
  public static final String CALLNUMBER_PREFIX_FIELD_PATH = "$.holdings[*].callNumberPrefix";
  public static final String CALLNUMBER_SUFFIX_FIELD_ID = "callNumberSuffix";
  public static final String CALLNUMBER_SUFFIX_FIELD_PATH = "$.holdings[*].callNumberSuffix";
  public static final String MATERIAL_TYPE_ID_PATH = "$.holdings[*].items[*].materialTypeId";
  public static final String ITEMS_ELECTRONIC_ACCESS_URI_PATH = "$.holdings[*].items[*].electronicAccess[*].uri";
  public static final String ITEMS_ELECTRONIC_ACCESS_LINK_TEXT_PATH = "$.holdings[*].items[*].electronicAccess[*].linkText";
  public static final String ITEMS_EFFECTIVE_CALL_NUMBER_PATH = "$.holdings[*].items[*].effectiveCallNumberComponents.callNumber";
  public static final String HOLDINGS_ELECTRONIC_ACCESS_URI_PATH = "$.holdings[*].electronicAccess[*].uri";
  public static final String HOLDINGS_ELECTRONIC_ACCESS_LINK_TEXT_PATH = "$.holdings[*].electronicAccess[*].linkText";
  public static final String MATERIALTYPE_FIELD_ID = "item.materialtypeid";
  public static final String EFFECTIVECALLNUMBER_CALL_NUMBER_FIELD_ID = "effectiveCallNumberComponents.callNumber";
  public static final String ELECTRONIC_ACCESS_URI_FIELD_ID = "electronicAccess.uri";
  public static final String ELECTRONIC_ACCESS_LINKTEXT_FIELD_ID = "electronicAccess.linkText";
  public static final String INSTANCE_HR_ID_FIELD_ID = "instance.hrid";
  public static final String INSTANCE_HR_ID_FIELD_PATH = "$.instance.hrid";
  public static final String INSTANCE_METADATA_UPDATED_DATE_FIELD_ID = "instance.metadata.updateddate";
  public static final String INSTANCE_METADATA_UPDATED_DATE_FIELD_PATH = "$.instance.metadata.updatedDate";
  public static final String INSTANCE_METADATA_CREATED_DATE_FIELD_ID = "instance.metadata.createddate";
  public static final String INSTANCE_METADATA_CREATED_DATE_FIELD_PATH = "$.instance.metadata.createdDate";
  public static final String INSTANCE_ELECTRONIC_ACCESS_URI_FIELD_ID = "instance.electronic.access.uri.resource";
  public static final String INSTANCE_ELECTRONIC_ACCESS_URI_FIELD_PATH = "$.instance.electronicAccess[?(@.relationshipId=='f5d0068e-6272-458e-8a81-b85e7b9a14aa')].uri";
  public static final String INSTANCE_ELECTRONIC_ACCESS_LINK_TEXT_FIELD_ID = "instance.electronic.access.linktext.resource";
  public static final String INSTANCE_ELECTRONIC_ACCESS_LINK_TEXT_PATH = "$.instance.electronicAccess[?(@.relationshipId=='f5d0068e-6272-458e-8a81-b85e7b9a14aa')].linkText";
}
