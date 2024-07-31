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
  public static final String AUTHORITY_DELETED_JOB_PROFILE_ID = "2c9be114-6d35-4408-adac-9ead35f51a27";
  public static final String DELETED_MARC_IDS_FILE_NAME = "deleted-marc-bib-records.csv";
  public static final String DELETED_AUTHORITIES_FILE_NAME = "deleted-authority-records.csv";
}
