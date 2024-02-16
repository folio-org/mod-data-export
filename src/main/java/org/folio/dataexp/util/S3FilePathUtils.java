package org.folio.dataexp.util;

import java.util.UUID;

import static org.folio.dataexp.util.Constants.TEMP_DIR_FOR_EXPORTS_BY_JOB_EXECUTION_ID;

public class S3FilePathUtils {


  private static final String SLICED_FILE_LOCATION_PATH = TEMP_DIR_FOR_EXPORTS_BY_JOB_EXECUTION_ID + "%s";
  public static final String PATTERN_TO_SAVE_FILE = "mod-data-export/upload/%s/%s";

  private S3FilePathUtils() {
  }

  public static String getPathToStoredFiles(String jobExecutionId, String fileName) {
    return String.format(SLICED_FILE_LOCATION_PATH, jobExecutionId, fileName);
  }

  public static String getPathToStoredFiles(UUID jobExecutionId, String fileName) {
    return getPathToStoredFiles(jobExecutionId.toString(), fileName);
  }

  public static String getPathToUploadedFiles(String fileDefinitionId, String fileName) {
    return String.format(PATTERN_TO_SAVE_FILE, fileDefinitionId, fileName);
  }

  public static String getPathToUploadedFiles(UUID fileDefinitionId, String fileName) {
    return getPathToUploadedFiles(fileDefinitionId.toString(), fileName);
  }
}
