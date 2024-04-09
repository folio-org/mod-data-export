package org.folio.dataexp.util;

import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public class S3FilePathUtils {

  public static final String TEMP_DIR_FOR_EXPORTS_BY_JOB_EXECUTION_ID = "mod-data-export/download/%s/";
  private static final String SLICED_FILE_LOCATION_PATH = TEMP_DIR_FOR_EXPORTS_BY_JOB_EXECUTION_ID + "%s";
  private static final String PATTERN_TO_SAVE_FILE = "mod-data-export/upload/%s/%s";

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

  public static String getTempDirForJobExecutionId(String exportTmpStorage, UUID jobExecutionId) {
    var tempDir = String.format(TEMP_DIR_FOR_EXPORTS_BY_JOB_EXECUTION_ID, jobExecutionId);
    if (StringUtils.isNotEmpty(exportTmpStorage)) {
      return exportTmpStorage + "/" + tempDir;
    }
    return tempDir;
  }

  public static String getLocalStorageWriterPath(String exportTmpStorage, String location) {
    if (StringUtils.isNotEmpty(exportTmpStorage)) {
      return exportTmpStorage + "/" + location;
    }
    return location;
  }
}
