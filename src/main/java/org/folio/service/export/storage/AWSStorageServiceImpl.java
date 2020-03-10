package org.folio.service.export.storage;

import java.util.Date;

import org.drools.core.util.StringUtils;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

/**
 * Retrieve files that are stored in AWS S3
 */
@Service
public class AWSStorageServiceImpl implements ExportStorageService {


  private final AmazonClient amazonClient;

  public AWSStorageServiceImpl(@Autowired AmazonClient amazonClient) {
    this.amazonClient = amazonClient;
  }

  /**
   * Fetch the link to download a file for a given job by fileName
   *
   * @param jobId:    The job to which the files are associated
   * @param fileName: The name of the file to download
   * @param tenantId
   * @return A link using which the file can be downloaded
   */
  @Override
  public String getFileDownloadLink(String jobId, String exportFileId, String tenantId) {
    String keyName = tenantId + "/" + jobId + "/" + exportFileId + ".mrc";

    GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(System.getProperty("bucket.name"),
      keyName).withMethod(HttpMethod.GET)
      .withExpiration(getExpiration());
    return amazonClient.generatePresignedUrl(generatePresignedUrlRequest).toString();
  }

  private Date getExpiration() {
    Date expiration = new Date();
    long expTimeMillis = expiration.getTime();
    expTimeMillis += 1000 * 60 * 60;
    expiration.setTime(expTimeMillis);

    return expiration;
  }

  @Override
  public void storeFile(FileDefinition fileDefinition) {
    String bucketName = System.getProperty("bucket.name");
    if (StringUtils.isEmpty(bucketName)) {
      throw new RuntimeException("S3 bucket name is not defined. Please set the bucket.name system property");
    }
    amazonClient.storeFile(bucketName, fileDefinition);
  }


}
