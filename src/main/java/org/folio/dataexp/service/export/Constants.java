package org.folio.dataexp.service.export;

import lombok.experimental.UtilityClass;

/**
 * Constants used for export operations.
 */
@UtilityClass
public class Constants {

  /** Default buffer size for output streams. */
  public static final int OUTPUT_BUFFER_SIZE = 8192;
  /** Default instance mapping profile ID. */
  public static final String DEFAULT_INSTANCE_MAPPING_PROFILE_ID =
      "25d81cbe-9686-11ea-bb37-0242ac130002";

  /** Key for record ID. */
  public static final String ID_KEY = "id";
  /** Key for instance record. */
  public static final String INSTANCE_KEY = "instance";
  /** Key for instance HRID. */
  public static final String INSTANCE_HRID_KEY = "instanceHrId";
  /** Key for HRID. */
  public static final String HRID_KEY = "hrid";
  /** Key for title. */
  public static final String TITLE_KEY = "title";
  /** Key for holdings. */
  public static final String HOLDINGS_KEY = "holdings";
  /** Key for items. */
  public static final String ITEMS_KEY = "items";
  /** Key for deleted flag. */
  public static final String DELETED_KEY = "deleted";
}
