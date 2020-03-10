package org.folio.service.export.storage;

import java.net.URL;

import org.folio.rest.jaxrs.model.FileDefinition;

import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

public interface AmazonClient {

  URL generatePresignedUrl(GeneratePresignedUrlRequest generatePresignedUrlRequest);

  /**
   * Save the files in a given folder into S3. They will be created in a subfolder with the name specified in prefix
   */
  void storeFile(String bucketName, FileDefinition fileDefinition);

}
