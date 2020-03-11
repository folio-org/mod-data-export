package org.folio.service.export.storage;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import org.drools.core.util.StringUtils;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.nio.file.Paths;
import java.util.Date;

import static java.lang.System.getProperty;

/**
 * Retrieve files that are stored in AWS S3
 */
@Service
public class AWSStorageServiceImpl implements ExportStorageService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int DOWNLOAD_LINK_EXPIRATION = 1000 * 60 * 60;

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
    String keyName = tenantId + "/" + jobId + "/" + exportFileId + ".mrc";
    GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(getProperty("bucket.name"), keyName)
      .withMethod(HttpMethod.GET)
      .withExpiration(getExpiration());
    return amazonFactory.getS3Client().generatePresignedUrl(request).toString();
  }

  private Date getExpiration() {
    Date expiration = new Date();
    long expTimeMillis = expiration.getTime();
    expTimeMillis += DOWNLOAD_LINK_EXPIRATION;
    expiration.setTime(expTimeMillis);
    return expiration;
  }

  @Override
  public void storeFile(FileDefinition fileDefinition) {
    String bucketName = getProperty("bucket.name");
    if (StringUtils.isEmpty(bucketName)) {
      throw new RuntimeException("S3 bucket name is not defined. Please set the bucket.name system property");
    }
    TransferManager transferManager = amazonFactory.getTransferManager();
    try {
      LOGGER.info("Uploading generated binary file {} to bucket {}", fileDefinition, bucketName);

      MultipleFileUpload multipleFileUpload = transferManager.uploadDirectory(
        bucketName,
        "data-export",
        Paths.get(fileDefinition.getSourcePath()).getParent().toFile(),
        false);
      multipleFileUpload.waitForCompletion();
    } catch (AmazonServiceException e) {
      LOGGER.error(e.getErrorMessage());
    } catch (InterruptedException e) {
      LOGGER.error(e.getMessage());
    } finally {
      transferManager.shutdownNow();
    }
  }
}
