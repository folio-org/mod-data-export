package org.folio.dataexp.service.export.storage;

import org.folio.dataexp.BaseDataExportInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertSame;

public class FolioS3ClientFactoryTest extends BaseDataExportInitializer {

  @Autowired
  private FolioS3ClientFactory folioS3ClientFactory;

  @Test
  void getFolioS3ClientTest() {
    var s3Client1= folioS3ClientFactory.getFolioS3Client();
    var s3Client2= folioS3ClientFactory.getFolioS3Client();
    assertSame(s3Client1, s3Client2);
  }
}
