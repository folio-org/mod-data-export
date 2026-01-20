package org.folio.dataexp.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
  public static final String QUERY_CQL_ALL_RECORDS = "(cql.allRecords=1)";
  public static final String INVALID_EXTENSION_ERROR_CODE = "error.uploadedFile.invalidExtension";
  public static final String ERROR_MESSAGE_PLACEHOLDER_CODE = "error.messagePlaceholder";
  public static final String OKAPI_URL = "http://_";
  public static final String COMMA = ",";
  public static final String DATE_PATTERN = "yyyyMMdd";
  public static final String DEFAULT_INSTANCE_JOB_PROFILE_ID = "6f7f3cd7-9f24-42eb-ae91-91af1cd54d0a";
  public static final String DEFAULT_AUTHORITY_DELETED_JOB_PROFILE_ID = "2c9be114-6d35-4408-adac-9ead35f51a27";
  public static final String DEFAULT_AUTHORITY_JOB_PROFILE_ID = "56944b1c-f3f9-475b-bed0-7387c33620ce";
  public static final String DEFAULT_HOLDINGS_JOB_PROFILE_ID = "5e9835fc-0e51-44c8-8a47-f7b8fce35da7";
  public static final String DELETED_MARC_IDS_FILE_NAME = "deleted-marc-bib-records.csv";
  public static final String DELETED_AUTHORITIES_FILE_NAME = "deleted-authority-records.csv";
  public static final String INVENTORY_VIEW_PERMISSION = "ui-inventory.instance.view";
  public static final String MSG_TEMPLATE_COULD_NOT_FIND_INSTANCE_BY_ID =
    "Couldn't find instance in db for ID: %s";
  public static final String STATE_ACTUAL = "ACTUAL";
}
