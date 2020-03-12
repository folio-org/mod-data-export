package org.folio.service.export.storage;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.util.StringUtils;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Date;
import org.folio.rest.exceptions.HttpException;
import org.folio.util.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Store and Retrieve files that are stored in AWS S3
 */
@Service
public class AWSStorageServiceImpl implements ExportStorageService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup()
    .lookupClass());
  private static final int EXPIRATION_TEN_MINUTES = 1000 * 10 * 60;

  @Autowired
  AWSClient awsClient;

  /**
   * Fetch the link to download a file for a given job by fileName
   *
   * @param jobExecutionId The job to which the files are associated
   * @param exportFileName   The name of the file to download
   * @param tenantId
   * @return A link using which the file can be downloaded
   */
  @Override
  public Future<String> getFileDownloadLink(String jobExecutionId, String exportFileName, String tenantId) {
    Promise<String> promise = Promise.promise();
    AmazonS3 s3Client = awsClient.getAWSS3Client();
    String keyName = tenantId + "/" + jobExecutionId + "/" + exportFileName;
    String bucketName = System.getProperty("bucket.name");
    URL url = null;

    if (StringUtils.isNullOrEmpty(bucketName)) {
      throw new HttpException(400, ErrorCodes.S3_BUCKET_NOT_PROVIDED);
    }

    GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, keyName)
      .withMethod(HttpMethod.GET)
      .withExpiration(getExpiration());
    try {
      url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
      promise.complete(url.toString());

    } catch (Exception e) {
      promise.fail(e);
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
  public void storeFile() {
    // Will be implemented as part of MDEXP-67
  }

}
