package org.folio.service.export;

import org.folio.rest.jaxrs.model.Bucket;

import com.amazonaws.SdkClientException;

public interface AmazonClient {

  /**
   * Create a bucket in S3.
   *
   * @param bucketName - name of the bucket to be created.
   * @return in case of any errors, empty optional to be returned.
   * @throws SdkClientException - throw SdkClientException in case of eny errors communicating with AWS, e.g credentials misconfiguration
   */
  Bucket createS3Bucket(String bucketName) throws SdkClientException;
}
