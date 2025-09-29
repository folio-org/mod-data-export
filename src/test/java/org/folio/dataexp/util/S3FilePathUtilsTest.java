package org.folio.dataexp.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class S3FilePathUtilsTest {

  @Test
  void getTempDirForJobExecutionIdTest() {
    var uuid = UUID.fromString("7aadccbf-63a1-4c66-bb3a-83365649236c");
    assertEquals("tmp/mod-data-export/download/7aadccbf-63a1-4c66-bb3a-83365649236c/",
        S3FilePathUtils.getTempDirForJobExecutionId("tmp", uuid));
    assertEquals("mod-data-export/download/7aadccbf-63a1-4c66-bb3a-83365649236c/",
        S3FilePathUtils.getTempDirForJobExecutionId(null, uuid));
  }

  @Test
  void getLocalStorageWriterPath() {
    assertEquals("tmp/fileLocation", S3FilePathUtils.getLocalStorageWriterPath(
        "tmp", "fileLocation"));
    assertEquals("fileLocation", S3FilePathUtils.getLocalStorageWriterPath(
        null, "fileLocation"));
  }
}
