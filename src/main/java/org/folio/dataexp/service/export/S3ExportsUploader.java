package org.folio.dataexp.service.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
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

  public String upload(UUID jobExecutionId, List<JobExecutionExportFilesEntity> exports, String initialFileName) {
    try {
      String uploadedPath;
      if (exports.size() > 1) {
        uploadedPath = uploadZip(jobExecutionId, exports, initialFileName);
      } else {
        uploadedPath =  uploadMarc(jobExecutionId, exports, initialFileName);
      }
      FileUtils.deleteDirectory(new File(String.format(TEMP_DIR_FOR_EXPORTS_BY_JOB_EXECUTION_ID, jobExecutionId)));
      return uploadedPath;
    } catch (IOException e) {
      throw new S3ExportsUploadException(e.getMessage());
    }
  }

  private String uploadMarc(UUID jobExecutionId, List<JobExecutionExportFilesEntity> exports, String fileName) throws IOException {
    var s3Name =  String.format("%s-%s.mrc", fileName, jobExecutionId);
    var s3path = getPathToStoredFiles(jobExecutionId, s3Name);
    var fileToUpload =  new File(exports.get(0).getFileLocation());
    if (fileToUpload.length() > 0) {
      try (var inputStream = new BufferedInputStream(new FileInputStream(fileToUpload))) {
        s3Client.write(s3path, inputStream);
      }
      log.info(exports.get(0).getFileLocation() + " uploaded as " + s3Name);
      FileUtils.delete(fileToUpload);
    } else {
      FileUtils.delete(fileToUpload);
      throw new S3ExportsUploadException("Marc file for exports is empty");
    }
    return s3path;
  }

  private String uploadZip (UUID jobExecutionId, List<JobExecutionExportFilesEntity> exports, String fileName) throws IOException {
    var zipName = fileName + ".zip";
    var zip = Files.createTempFile(FilenameUtils.getName(zipName), FilenameUtils.getExtension(zipName)).toFile();
    boolean isZipFileNotEmpty = false;
    try (var fileOutputStream = new FileOutputStream(zip); var zipOutputStream = new ZipOutputStream(fileOutputStream)) {
      var countExportsFiles = 0;
      for (var export : exports) {
        var exportFile = new File(export.getFileLocation());
        if (exportFile.length() == 0) {
          continue;
        }
        countExportsFiles++;
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(exportFile))) {
          var zipEntryName = String.format("%s-%s-%s.mrc", fileName, jobExecutionId, countExportsFiles);
          log.info(export.getFileLocation() + " zipped as " + zipEntryName);
          ZipEntry zipEntry = new ZipEntry(zipEntryName);
          zipOutputStream.putNextEntry(zipEntry);
          byte[] bytes = new byte[1024];
          int length;
          while ((length = inputStream.read(bytes)) > 0) {
            zipOutputStream.write(bytes, 0, length);
          }
        }
        isZipFileNotEmpty = true;
        Files.delete(exportFile.toPath());
      }
    }
    var s3ZipPath = getPathToStoredFiles(jobExecutionId, zipName);
    if (isZipFileNotEmpty) {
      try (var inputStream = new BufferedInputStream(new FileInputStream(zip))) {
        s3Client.write(s3ZipPath, inputStream);
      }
    } else {
      FileUtils.delete(zip);
      throw new S3ExportsUploadException("Zip file for exports is empty");
    }
    FileUtils.delete(zip);
    return s3ZipPath;
  }
}
