package org.folio.dataexp.service.export;

import java.util.Set;

/**
 * Constants used for export operations.
 */
public class Constants {

  private Constants() {}

  /** Default buffer size for output streams. */
  public static final int OUTPUT_BUFFER_SIZE = 8192;
  /** Default instance mapping profile ID. */
  public static final String DEFAULT_INSTANCE_MAPPING_PROFILE_ID =
      "25d81cbe-9686-11ea-bb37-0242ac130002";
  /** Default holdings mapping profile ID. */
  public static final String DEFAULT_HOLDINGS_MAPPING_PROFILE_ID =
      "1ef7d0ac-f0a8-42b5-bbbb-c7e249009c13";
  /** Default authority mapping profile ID. */
  public static final String DEFAULT_AUTHORITY_MAPPING_PROFILE_ID =
      "5d636597-a59d-4391-a270-4e79d5ba70e3";
  /** Default linked data mapping profile ID. */
  public static final String DEFAULT_LINKED_DATA_MAPPING_PROFILE_ID =
      "f8b400da-6a0c-4058-be10-cece93265c32";
  /** List of default mapping profiles. */
  public static final Set<String> DEFAULT_MAPPING_PROFILE_IDS = Set.of(
      DEFAULT_INSTANCE_MAPPING_PROFILE_ID,
      DEFAULT_HOLDINGS_MAPPING_PROFILE_ID,
      DEFAULT_AUTHORITY_MAPPING_PROFILE_ID,
      DEFAULT_LINKED_DATA_MAPPING_PROFILE_ID
  );

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
