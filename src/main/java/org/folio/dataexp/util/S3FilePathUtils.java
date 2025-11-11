package org.folio.dataexp.util;

import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for building S3 file paths for data export and upload operations.
 */
public class S3FilePathUtils {

  /** Template for temporary export directory by job execution ID. */
  private static final String TEMP_DIR_FOR_EXPORTS_BY_JOB_EXECUTION_ID =
      "mod-data-export/download/%s/";
  /** Template for sliced file location path. */
  private static final String SLICED_FILE_LOCATION_PATH =
      TEMP_DIR_FOR_EXPORTS_BY_JOB_EXECUTION_ID + "%s";
  /** Template for uploaded file path. */
  private static final String PATTERN_TO_SAVE_FILE = "mod-data-export/upload/%s/%s";
  /** Template for record location path. */
  public static final String RECORD_LOCATION_PATH = "mod-data-export/download/%s/%s";

  /**
   * Private constructor to prevent instantiation.
   */
  private S3FilePathUtils() {
  }

  /**
   * Returns the path to stored files for a given job execution ID and file name.
   *
   * @param jobExecutionId the job execution ID as a string
   * @param fileName the file name
   * @return the full path to the stored file
   */
  public static String getPathToStoredFiles(String jobExecutionId, String fileName) {
    return String.format(SLICED_FILE_LOCATION_PATH, jobExecutionId, fileName);
  }

  /**
   * Returns the path to stored files for a given job execution UUID and file name.
   *
   * @param jobExecutionId the job execution ID as a UUID
   * @param fileName the file name
   * @return the full path to the stored file
   */
  public static String getPathToStoredFiles(UUID jobExecutionId, String fileName) {
    return getPathToStoredFiles(jobExecutionId.toString(), fileName);
  }

  /**
   * Returns the path to uploaded files for a given file definition ID and file name.
   *
   * @param fileDefinitionId the file definition ID as a string
   * @param fileName the file name
   * @return the full path to the uploaded file
   */
  public static String getPathToUploadedFiles(String fileDefinitionId, String fileName) {
    return String.format(PATTERN_TO_SAVE_FILE, fileDefinitionId, fileName);
  }

  /**
   * Returns the path to uploaded files for a given file definition UUID and file name.
   *
   * @param fileDefinitionId the file definition ID as a UUID
   * @param fileName the file name
   * @return the full path to the uploaded file
   */
  public static String getPathToUploadedFiles(UUID fileDefinitionId, String fileName) {
    return getPathToUploadedFiles(fileDefinitionId.toString(), fileName);
  }

  /**
   * Returns the temporary directory path for a given job execution ID.
   *
   * @param exportTmpStorage the export temporary storage base path
   * @param jobExecutionId the job execution ID as a UUID
   * @return the temporary directory path
   */
  public static String getTempDirForJobExecutionId(String exportTmpStorage, UUID jobExecutionId) {
    var tempDir = String.format(TEMP_DIR_FOR_EXPORTS_BY_JOB_EXECUTION_ID, jobExecutionId);
    if (StringUtils.isNotEmpty(exportTmpStorage)) {
      return exportTmpStorage + "/" + tempDir;
    }
    return tempDir;
  }

  /**
   * Returns the local storage writer path for a given export temporary storage and location.
   *
   * @param exportTmpStorage the export temporary storage base path
   * @param location the file location
   * @return the full local storage writer path
   */
  public static String getLocalStorageWriterPath(String exportTmpStorage, String location) {
    if (StringUtils.isNotEmpty(exportTmpStorage)) {
      return exportTmpStorage + "/" + location;
    }
    return location;
  }

  /**
   * Returns the path to a stored record for a given directory name and file name.
   *
   * @param dirName the directory name
   * @param fileName the file name
   * @return the full path to the stored record
   */
  public static String getPathToStoredRecord(String dirName, String fileName) {
    return String.format(RECORD_LOCATION_PATH, dirName, fileName);
  }

  /**
   * Returns the file suffix based on the output format.
   *
   * @param outputFormat string form of MappingProfile.OutputFormatEnum
   * @return file suffix
   */
  public static String getFileSuffixFromOutputFormat(String outputFormat) {
    return switch (outputFormat) {
      case "LINKED_DATA" -> Constants.LINKED_DATA_FILE_SUFFIX;
      case "MARC" -> Constants.MARC_FILE_SUFFIX;
      default -> Constants.MARC_FILE_SUFFIX;
    };
  }
}
