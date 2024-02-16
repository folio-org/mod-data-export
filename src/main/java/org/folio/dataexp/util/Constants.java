package org.folio.dataexp.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
  public static final String TEMP_DIR_FOR_EXPORTS_BY_JOB_EXECUTION_ID = "mod-data-export/download/%s/";
  public static final String QUERY_CQL_ALL_RECORDS = "(cql.allRecords=1)";
  public static final String INVALID_EXTENSION_ERROR_CODE = "error.uploadedFile.invalidExtension";
  public static final String ERROR_MESSAGE_PLACEHOLDER_CODE = "error.messagePlaceholder";
}
