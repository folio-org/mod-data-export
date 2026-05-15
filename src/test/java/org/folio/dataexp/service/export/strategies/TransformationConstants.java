package org.folio.dataexp.service.export.strategies;

/**
 * Constants for transformation field IDs and JSON paths used in holdings and item export
 * strategies.
 *
 * <p>This class provides string constants for field identifiers, JSON path expressions, and
 * function names used during transformation of holdings and item data in export processes.
 */
public final class TransformationConstants {

  public static final String PERMANENT_LOCATION_FIELD_ID = "holdings.permanentlocation.name";
  public static final String PERMANENT_LOCATION_CODE_FIELD_ID = "holdings.permanentlocation.code";
  public static final String PERMANENT_LOCATION_LIBRARY_NAME_FIELD_ID =
      "holdings.permanentlocation.library.name";
  public static final String PERMANENT_LOCATION_LIBRARY_CODE_FIELD_ID =
      "holdings.permanentlocation.library.code";
  public static final String PERMANENT_LOCATION_CAMPUS_NAME_FIELD_ID =
      "holdings.permanentlocation.campus.name";
  public static final String PERMANENT_LOCATION_CAMPUS_CODE_FIELD_ID =
      "holdings.permanentlocation.campus.code";
  public static final String PERMANENT_LOCATION_INSTITUTION_NAME_FIELD_ID =
      "holdings.permanentlocation.institution.name";
  public static final String PERMANENT_LOCATION_INSTITUTION_CODE_FIELD_ID =
      "holdings.permanentlocation.institution.code";
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
}
