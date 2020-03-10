package org.folio.service.export.storage;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.util.StringUtils;
import java.net.URL;
import java.util.Date;
import org.folio.rest.exceptions.HttpException;
import org.folio.util.ErrorCodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Store and Retrieve files that are stored in AWS S3
 */
@Service
public class AWSStorageServiceImpl implements ExportStorageService {

  @Autowired
  AWSClient awsClient;

  /**
   * Fetch the link to download a file for a given job by fileName
   *
   * @param jobExecutionId The job to which the files are associated
   * @param exportFileId   The id of the file to download
   * @param tenantId
   * @return A link using which the file can be downloaded
   */
  @Override
  public String getFileDownloadLink(String jobExecutionId, String exportFileId, String tenantId) {
    AmazonS3 s3Client = awsClient.getAWSS3Client();
    String keyName = tenantId + "/" + jobExecutionId + "/" + exportFileId + ".mrc";
    String bucketName = System.getProperty("bucket.name");

    if (StringUtils.isNullOrEmpty(bucketName)) {
      throw new HttpException(400, ErrorCodes.S3_BUCKET_NOT_PROVIDED);
    }

    GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, keyName)
      .withMethod(HttpMethod.GET)
      .withExpiration(getExpiration());
    URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);

    return url.toString();

  }

  /**
   * Returns the expiration time, which is 10 minutes after current time
   *
   * @return Date expiration date
   */
  private Date getExpiration() {
    Date expiration = new Date();
    long expTimeMillis = expiration.getTime();
    expTimeMillis += 1000 * 10 * 60;
    expiration.setTime(expTimeMillis);

    return expiration;
  }

  @Override
  public void storeFile() {
    // Will be implemented as part of MDEXP-67

  }

}
