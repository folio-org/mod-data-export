package org.folio.dataexp.service.transformationfields;

/**
 * Constants for JSON paths used in transformation fields.
 */
public class Constants {

  private Constants() {}

  /** JSON path for permanent location ID in holdings items. */
  public static final String HOLDINGS_ITEMS_PERMANENT_LOCATION_ID_PATH =
      "$.holdings[*].items[*].permanentLocationId";
  /** JSON path for effective location ID in holdings items. */
  public static final String HOLDINGS_ITEMS_EFFECTIVE_LOCATION_ID_PATH =
      "$.holdings[*].items[*].effectiveLocationId";
}
