package org.folio.service.export.storage;

import static java.lang.System.getProperty;

import java.lang.invoke.MethodHandles;
import java.nio.file.Paths;

import org.drools.core.util.StringUtils;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;

/**
 * Retrieve files that are stored in AWS S3
 */
@Service
public class AWSStorageServiceImpl implements ExportStorageService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String MARC_FILE_EXTENSION = "mrc";

  @Autowired
  private AmazonFactory amazonFactory;

  /**
   * Fetch the link to download a file for a given job by fileName
   *
   * @param jobId:   The job to which the files are associated
   * @param tenantId
   * @return A link using which the file can be downloaded
   */
  @Override
  public String getFileDownloadLink(String jobId, String exportFileId, String tenantId) {
    return StringUtils.EMPTY;
  }

  @Override
  public void storeFile(FileDefinition fileDefinition, String tenantId) {
    String parentFolder = String.format("%s/%s/%s.%s", tenantId, fileDefinition.getJobExecutionId(), fileDefinition.getId(), MARC_FILE_EXTENSION);
    String bucketName = getProperty("bucket.name");
    if (StringUtils.isEmpty(bucketName)) {
      throw new IllegalStateException("S3 bucket name is not defined. Please set the bucket.name system property");
    } else {
      TransferManager transferManager = amazonFactory.getTransferManager();
      try {
        LOGGER.info("Uploading generated binary file {} to bucket {}", fileDefinition, bucketName);
        MultipleFileUpload multipleFileUpload = transferManager.uploadDirectory(
          bucketName,
          parentFolder,
          Paths.get(fileDefinition.getSourcePath()).getFileName().toFile(),
          false);
        multipleFileUpload.waitForCompletion();
      } catch (InterruptedException e) {
        LOGGER.error(e.getMessage());
        Thread.currentThread().interrupt();
      } finally {
        transferManager.shutdownNow();
      }
    }
  }
}
