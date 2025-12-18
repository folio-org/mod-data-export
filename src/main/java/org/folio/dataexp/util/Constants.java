package org.folio.dataexp.util;

import lombok.experimental.UtilityClass;

/** Utility class containing constant values used throughout the data export module. */
@UtilityClass
public class Constants {
  /** CQL query for selecting all records. */
  public static final String QUERY_CQL_ALL_RECORDS = "(cql.allRecords=1)";

  /** Error code for invalid uploaded file extension. */
  public static final String INVALID_EXTENSION_ERROR_CODE = "error.uploadedFile.invalidExtension";

  /** Error code for message placeholders. */
  public static final String ERROR_MESSAGE_PLACEHOLDER_CODE = "error.messagePlaceholder";

  /** Default Okapi URL placeholder. */
  public static final String OKAPI_URL = "http://_";

  /** Comma character. */
  public static final String COMMA = ",";

  /** Date pattern used for formatting dates. */
  public static final String DATE_PATTERN = "yyyyMMdd";

  /** Default job profile ID for instance exports. */
  public static final String DEFAULT_INSTANCE_JOB_PROFILE_ID =
      "6f7f3cd7-9f24-42eb-ae91-91af1cd54d0a";

  /** Default job profile ID for deleted authority exports. */
  public static final String DEFAULT_AUTHORITY_DELETED_JOB_PROFILE_ID =
      "2c9be114-6d35-4408-adac-9ead35f51a27";

  /** Default job profile ID for authority exports. */
  public static final String DEFAULT_AUTHORITY_JOB_PROFILE_ID =
      "56944b1c-f3f9-475b-bed0-7387c33620ce";

  /** Default job profile ID for holdings exports. */
  public static final String DEFAULT_HOLDINGS_JOB_PROFILE_ID =
      "5e9835fc-0e51-44c8-8a47-f7b8fce35da7";

  /** File name for deleted MARC bibliographic records. */
  public static final String DELETED_MARC_IDS_FILE_NAME = "deleted-marc-bib-records.csv";

  /** File name for deleted authority records. */
  public static final String DELETED_AUTHORITIES_FILE_NAME = "deleted-authority-records.csv";

  /** Permission string for viewing inventory instances. */
  public static final String INVENTORY_VIEW_PERMISSION = "ui-inventory.instance.view";

  /** File suffix for MARC records. */
  public static final String MARC_FILE_SUFFIX = "mrc";

  /** File suffix for Linked Data records. */
  public static final String LINKED_DATA_FILE_SUFFIX = "json";

  /** Standard file path pattern: original filename, specifier, and suffix. */
  public static final String FILE_NAME_FORMAT = "%s-%s.%s";

  public static final String STATE_ACTUAL = "ACTUAL";
  public static final String STATE_DELETED = "DELETED";
  public static final char LEADER_STATUS_DELETED = 'd';

  public static final String MSG_TEMPLATE_COULD_NOT_FIND_INSTANCE_BY_ID =
      "Couldn't find instance in db for ID: %s";
}
