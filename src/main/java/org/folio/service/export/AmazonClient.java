package org.folio.service.export;

import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import org.folio.rest.jaxrs.model.Bucket;

import com.amazonaws.SdkClientException;
import org.folio.rest.jaxrs.model.FileDefinition;

import java.net.URL;

public interface AmazonClient {

  /**
   * Create a bucket in S3.
   *
   * @param bucketName - name of the bucket to be created.
   * @return in case of any errors, empty optional to be returned.
   * @throws SdkClientException - throw SdkClientException in case of eny errors communicating with AWS, e.g credentials misconfiguration
   */
  @Deprecated
  Bucket createS3Bucket(String bucketName) throws SdkClientException;

  void copy(String bucketName);

  URL generatePresignedUrl(GeneratePresignedUrlRequest generatePresignedUrlRequest);

  /**
   * Save the files in a given folder into S3. They will be created in a subfolder with the name specified in prefix
   */
  void saveFilesUsingTransferManager(String bucketName, FileDefinition fileDefinition);

}
