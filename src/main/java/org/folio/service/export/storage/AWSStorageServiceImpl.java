package org.folio.service.export.storage;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.util.StringUtils;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.folio.HttpStatus;
import org.folio.rest.exceptions.ServiceException;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.logs.ErrorLogService;
import org.folio.util.ErrorCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Date;

import static java.lang.System.getProperty;

/**
 * Saves files into Amazon cloud and provides an access for files being stored there
 */
@Service
public class AWSStorageServiceImpl implements ExportStorageService {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());
  private static final int EXPIRATION_TEN_MINUTES = 1000 * 10 * 60;
  private static final String BUCKET_PROP_KEY = "bucket.name";

  @Autowired
  private AmazonFactory amazonFactory;
  @Autowired
  private Vertx vertx;
  @Autowired
  private ErrorLogService errorLogService;

  /**
   * Fetch the link to download a file for a given job by fileName
   *
   * @param jobExecutionId The job to which the files are associated
   * @param exportFileName The name of the file to download
   * @param tenantId       tenant id
   * @return A link using which the file can be downloaded
   */
  @Override
  public Future<String> getFileDownloadLink(String jobExecutionId, String exportFileName, String tenantId) {
    Promise<String> promise = Promise.promise();
    AmazonS3 s3Client = amazonFactory.getS3Client();
    String keyName = tenantId + "/" + jobExecutionId + "/" + exportFileName;
    String bucketName = getProperty(BUCKET_PROP_KEY);
    if (StringUtils.isNullOrEmpty(bucketName)) {
      errorLogService.saveGeneralError(ErrorCode.S3_BUCKET_IS_NOT_PROVIDED.getCode(), jobExecutionId, tenantId);
      throw new ServiceException(HttpStatus.HTTP_INTERNAL_SERVER_ERROR, ErrorCode.S3_BUCKET_NAME_NOT_FOUND);
    }
    GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, keyName)
      .withMethod(HttpMethod.GET)
      .withExpiration(getExpiration());
    try {
      vertx.executeBlocking(blockingFuture -> {
        URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
        blockingFuture.complete(url.toString());
      }, asyncResult -> {
        if (asyncResult.failed()) {
          promise.fail(asyncResult.cause());
        } else {
          String url = (String) asyncResult.result();
          promise.complete(url);
        }
      });
    } finally {
      s3Client.shutdown();
    }
    return promise.future();
  }

  /**
   * Returns the expiration time, which is 10 minutes after current time
   *
   * @return Date expiration date
   */
  private Date getExpiration() {
    Date expiration = new Date();
    long expTimeMillis = expiration.getTime();
    expTimeMillis += EXPIRATION_TEN_MINUTES;
    LOGGER.info("AWS presigned URL link expiration time millis {}", expTimeMillis);
    expiration.setTime(expTimeMillis);
    return expiration;
  }


  @Override
  public void storeFile(FileDefinition fileDefinition, String tenantId) {
    String folderInS3 = tenantId + "/" + fileDefinition.getJobExecutionId();
    String bucketName = getProperty(BUCKET_PROP_KEY);
    if (StringUtils.isNullOrEmpty(bucketName)) {
      errorLogService.saveGeneralError(ErrorCode.S3_BUCKET_NAME_NOT_FOUND.getCode(), fileDefinition.getJobExecutionId(), tenantId);
      throw new ServiceException(HttpStatus.HTTP_INTERNAL_SERVER_ERROR, ErrorCode.S3_BUCKET_NAME_NOT_FOUND);
    } else {
      TransferManager transferManager = amazonFactory.getTransferManager();
      try {
        LOGGER.info("Uploading generated binary file {} to bucket {}", fileDefinition, bucketName);
        MultipleFileUpload multipleFileUpload = transferManager.uploadDirectory(
          bucketName,
          folderInS3,
          Paths.get(fileDefinition.getSourcePath()).getParent().toFile(),
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
