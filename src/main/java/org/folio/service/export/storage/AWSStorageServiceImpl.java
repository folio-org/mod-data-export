package org.folio.service.export.storage;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import java.net.URL;
import java.util.Date;
import org.springframework.stereotype.Service;

/**
 * Retrieve files that are stored in AWS S3
 */
@Service
public class AWSStorageServiceImpl implements ExportStorageService {

  AmazonS3 s3Client;


  /**
   * Fetch the link to download a file for a given job by fileName
   *
   * @param          jobId: The job to which the files are associated
   * @param          fileName: The name of the file to download
   * @param tenantId
   * @return A link using which the file can be downloaded
   */
  @Override
  public String getFileDownloadLink(String jobId, String exportFileId, String tenantId) {
    s3Client = AmazonS3ClientBuilder.defaultClient();
    String keyName = tenantId + "/" + jobId + "/" + exportFileId + ".mrc";

    GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(System.getProperty("bucket.name"),
        keyName).withMethod(HttpMethod.GET)
          .withExpiration(getExpiration());
    URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);

    return url.toString();

  }

  private Date getExpiration() {
    Date expiration = new Date();
    long expTimeMillis = expiration.getTime();
    expTimeMillis += 1000 * 60 * 60;
    expiration.setTime(expTimeMillis);

    return expiration;
  }

  @Override
  public void storeFile() {
    // Implement it

  }

}
