package org.folio.dataexp.service.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.entity.JobExecutionExportFilesEntity;
import org.folio.dataexp.exception.export.S3ExportsUploadException;
import org.folio.s3.client.FolioS3Client;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
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

import static org.folio.dataexp.util.Constants.TEMP_DIR_FOR_EXPORTS_BY_JOB_EXECUTION_ID;
import static org.folio.dataexp.util.S3FilePathUtils.getPathToStoredFiles;


@Component
@RequiredArgsConstructor
@Log4j2
public class S3ExportsUploader {

  private final FolioS3Client s3Client;
  private static final String EMPTY_FILE_FOR_EXPORT = "File for exports is empty";

  public String upload(JobExecution jobExecution, List<JobExecutionExportFilesEntity> exports, String initialFileName) {
    if (exports.isEmpty()) {
      throw new S3ExportsUploadException(EMPTY_FILE_FOR_EXPORT);
    }
    try {
      String uploadedPath;
      if (exports.size() > 1) {
        var filesToExport = exports.stream().map(e -> new File(e.getFileLocation()))
          .filter(f -> f.length() > 0).toList();
        if (filesToExport.size() > 1) {
          uploadedPath = uploadZip(jobExecution, filesToExport, initialFileName);
        } else if (filesToExport.size() == 1) {
          uploadedPath = uploadMarc(jobExecution, filesToExport.get(0), initialFileName);
        } else {
          removeTempDirForJobExecution(jobExecution.getId());
          throw new S3ExportsUploadException(EMPTY_FILE_FOR_EXPORT);
        }
      } else {
        var fileToExport = new File(exports.get(0).getFileLocation());
        uploadedPath = uploadMarc(jobExecution, fileToExport, initialFileName);
      }
      return uploadedPath;
    } catch (IOException e) {
      throw new S3ExportsUploadException(e.getMessage());
    }
  }

  private String uploadMarc(JobExecution jobExecution, File fileToUpload, String fileName) throws IOException {
    var s3Name =  String.format("%s-%s.mrc", fileName, jobExecution.getHrId());
    var s3path = getPathToStoredFiles(jobExecution.getId(), s3Name);
    if (fileToUpload.length() > 0) {
      try (var inputStream = new BufferedInputStream(new FileInputStream(fileToUpload))) {
        s3Client.write(s3path, inputStream);
      }
      log.info(fileToUpload.getPath() + " uploaded as " + s3Name);
      removeTempDirForJobExecution(jobExecution.getId());
    } else {
      removeTempDirForJobExecution(jobExecution.getId());
      throw new S3ExportsUploadException(EMPTY_FILE_FOR_EXPORT);
    }
    return s3path;
  }

  private String uploadZip (JobExecution jobExecution, List<File> exports, String fileName) throws IOException {
    var zipFileName = fileName + ".zip";
    var zipDirPath =  getTempDirForJobExecutionId(jobExecution.getId()) + "zip/";
    Files.createDirectories(Path.of(zipDirPath));
    var zipFilePath = zipDirPath + zipFileName;
    var zip = Files.createFile(Path.of(zipFilePath)).toFile();
    try (var fileOutputStream = new FileOutputStream(zip); var zipOutputStream = new ZipOutputStream(fileOutputStream)) {
      var countExportsFiles = 0;
      for (var exportFile : exports) {
        countExportsFiles++;
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(exportFile))) {
          var zipEntryName = String.format("%s-%s-%s.mrc", fileName, jobExecution.getHrId(), countExportsFiles);
          log.info(exportFile.getPath() + " zipped as " + zipEntryName);
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
        s3Client.write(s3ZipPath, inputStream);
    }
    FileUtils.delete(zip);
    removeTempDirForJobExecution(jobExecution.getId());
    return s3ZipPath;
  }

  private String getTempDirForJobExecutionId(UUID jobExecutionId) {
    return String.format(TEMP_DIR_FOR_EXPORTS_BY_JOB_EXECUTION_ID, jobExecutionId);
  }

  private void removeTempDirForJobExecution(UUID jobExecutionId) throws IOException {
    FileUtils.deleteDirectory(new File(getTempDirForJobExecutionId(jobExecutionId)));
  }
}
