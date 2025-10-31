package org.folio.dataexp.service.export;

import static org.folio.dataexp.util.S3FilePathUtils.getPathToStoredFiles;
import static org.folio.dataexp.util.S3FilePathUtils.getPathToStoredRecord;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.exception.export.S3ExportsUploadException;
import org.folio.dataexp.util.S3FilePathUtils;
import org.folio.s3.client.FolioS3Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Handles uploading exported files to S3 storage, including MARC, JSON, and ZIP files.
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class S3ExportsUploader {

  public static final String EMPTY_FILE_FOR_EXPORT_ERROR_MESSAGE = "File for exports is empty";
  private final FolioS3Client s3Client;
  private String exportTmpStorage;

  @Value("${application.export-tmp-storage}")
  protected void setExportTmpStorage(String exportTmpStorage) {
    this.exportTmpStorage = exportTmpStorage;
  }

  /**
   * Uploads exported files for a job execution to S3.
   *
   * @param jobExecution the job execution
   * @param exports list of export file entities
   * @param initialFileName initial file name
   * @param fileSuffix filename suffix
   * @return S3 path of the uploaded file
   */
  public String upload(JobExecution jobExecution, List<JobExecutionExportFilesEntity> exports,
      String initialFileName, String fileSuffix) {
    if (exports.isEmpty()) {
      throw new S3ExportsUploadException(EMPTY_FILE_FOR_EXPORT_ERROR_MESSAGE);
    }
    try {
      String uploadedPath;
      if (exports.size() > 1) {
        var filesToExport = exports.stream()
            .map(e -> new File(S3FilePathUtils.getLocalStorageWriterPath(
                exportTmpStorage, e.getFileLocation())))
            .filter(f -> f.length() > 0)
            .toList();
        if (filesToExport.size() > 1) {
          uploadedPath = uploadZip(jobExecution, filesToExport, initialFileName,
              fileSuffix);
        } else if (filesToExport.size() == 1) {
          uploadedPath = uploadSingleFile(jobExecution, filesToExport.get(0),
              initialFileName, fileSuffix);
        } else {
          removeTempDirForJobExecution(jobExecution.getId());
          throw new S3ExportsUploadException(EMPTY_FILE_FOR_EXPORT_ERROR_MESSAGE);
        }
      } else {
        var fileToExport = new File(S3FilePathUtils.getLocalStorageWriterPath(
            exportTmpStorage, exports.get(0).getFileLocation()));
        uploadedPath = uploadSingleFile(jobExecution, fileToExport, initialFileName, fileSuffix);
      }
      return uploadedPath;
    } catch (IOException e) {
      throw new S3ExportsUploadException(e.getMessage());
    }
  }

  /**
   * Uploads a single MARC record by ID to S3.
   *
   * @param dirName directory name
   * @param marcFileContentBytes MARC file content bytes
   * @throws IOException if upload fails
   */
  public void uploadSingleRecordById(String dirName, byte[] marcFileContentBytes)
      throws IOException {
    var s3FileName = "%s.mrc".formatted(dirName);
    var s3path = getPathToStoredRecord(dirName, s3FileName);
    try (var inputStream = new ByteArrayInputStream(marcFileContentBytes)) {
      s3Client.write(s3path, inputStream);
    }
    log.info("Marc record uploaded as " + s3FileName);
  }

  /**
   * Uploads a single file to S3.
   *
   * @param jobExecution the job execution
   * @param fileToUpload the file to upload
   * @param fileName the file name
   * @param fileSuffix filename suffix
   * @return S3 path of the uploaded file
   * @throws IOException if upload fails
   */
  private String uploadSingleFile(JobExecution jobExecution, File fileToUpload, String fileName,
      String fileSuffix) throws IOException {
    var s3Name = String.format("%s-%s.%s", fileName, jobExecution.getHrId(), fileSuffix);
    var s3path = getPathToStoredFiles(jobExecution.getId(), s3Name);
    if (fileToUpload.length() > 0) {
      try (var inputStream = new BufferedInputStream(new FileInputStream(fileToUpload))) {
        s3Client.write(s3path, inputStream, fileToUpload.length());
      }
      log.info(fileToUpload.getPath() + " uploaded as " + s3Name);
      removeTempDirForJobExecution(jobExecution.getId());
    } else {
      removeTempDirForJobExecution(jobExecution.getId());
      throw new S3ExportsUploadException(EMPTY_FILE_FOR_EXPORT_ERROR_MESSAGE);
    }
    return s3path;
  }

  /**
   * Uploads multiple files as a ZIP archive to S3.
   *
   * @param jobExecution the job execution
   * @param exports list of files to export
   * @param fileName the file name
   * @param fileSuffix member filename suffix
   * @return S3 path of the uploaded ZIP file
   * @throws IOException if upload fails
   */
  private String uploadZip(JobExecution jobExecution, List<File> exports, String fileName,
      String fileSuffix) throws IOException {
    var zipFileName = String.format("%s-%s.zip", fileName, jobExecution.getHrId());
    var zipDirPath = S3FilePathUtils.getTempDirForJobExecutionId(
        exportTmpStorage, jobExecution.getId()) + "zip/";
    Files.createDirectories(Path.of(zipDirPath));
    var zipFilePath = zipDirPath + zipFileName;
    var zip = Files.createFile(Path.of(zipFilePath)).toFile();
    try (var fileOutputStream = new FileOutputStream(zip);
         var zipOutputStream = new ZipOutputStream(fileOutputStream)) {
      var countExportsFiles = 0;
      for (var exportFile : exports) {
        countExportsFiles++;
        try (InputStream inputStream = new BufferedInputStream(
            new FileInputStream(exportFile))) {
          var zipEntryName = String.format(
              "%s-%s-%s.%s", fileName, jobExecution.getHrId(), countExportsFiles, fileSuffix);
          log.info(exportFile.getPath() + " add to zip as " + zipEntryName);
          ZipEntry zipEntry = new ZipEntry(zipEntryName);
          zipOutputStream.putNextEntry(zipEntry);
          byte[] bytes = new byte[1024];
          int length;
          while ((length = inputStream.read(bytes)) > 0) {
            zipOutputStream.write(bytes, 0, length);
          }
        }
        Files.delete(exportFile.toPath());
      }
    }
    var s3ZipPath = getPathToStoredFiles(jobExecution.getId(), zipFileName);
    try (var inputStream = new BufferedInputStream(new FileInputStream(zip))) {
      s3Client.write(s3ZipPath, inputStream, zip.length());
    }
    FileUtils.delete(zip);
    removeTempDirForJobExecution(jobExecution.getId());
    return s3ZipPath;
  }

  /**
   * Removes the temporary directory for a job execution.
   *
   * @param jobExecutionId the job execution ID
   * @throws IOException if removal fails
   */
  private void removeTempDirForJobExecution(UUID jobExecutionId) throws IOException {
    FileUtils.deleteDirectory(
        new File(S3FilePathUtils.getTempDirForJobExecutionId(exportTmpStorage, jobExecutionId)));
  }
}
