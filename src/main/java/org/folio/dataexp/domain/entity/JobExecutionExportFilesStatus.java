package org.folio.dataexp.domain.entity;

/**
 * Status of job execution export files.
 */
public enum JobExecutionExportFilesStatus {
  /** Export file is scheduled. */
  SCHEDULED,
  /** Export file is active. */
  ACTIVE,
  /** Export file is completed. */
  COMPLETED,
  /** Export file is completed with errors. */
  COMPLETED_WITH_ERRORS,
  /** Export file has failed. */
  FAILED;
}
