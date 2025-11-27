package org.folio.dataexp.service.export;

import static org.folio.dataexp.service.export.Constants.OUTPUT_BUFFER_SIZE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.util.S3FilePathUtils;
import org.junit.jupiter.api.Test;

class LocalStorageWriterTest {

  @Test
  @SneakyThrows
  void writeTest() {
    var jobExecutionId = UUID.randomUUID();
    var temDirLocation =
        S3FilePathUtils.getTempDirForJobExecutionId(StringUtils.EMPTY, jobExecutionId);
    Files.createDirectories(Path.of(temDirLocation));
    var fileLocation = temDirLocation + "marc.mrc";

    var writer = new LocalStorageWriter(fileLocation, OUTPUT_BUFFER_SIZE);
    writer.write("data");
    writer.close();
    var file = new File(fileLocation);
    assertTrue(file.length() > 0);

    FileUtils.deleteDirectory(new File(temDirLocation));
  }

  @Test
  @SneakyThrows
  void writeIfExceptionTest() {
    String invalidData = null;
    var jobExecutionId = UUID.randomUUID();
    var temDirLocation =
        S3FilePathUtils.getTempDirForJobExecutionId(StringUtils.EMPTY, jobExecutionId);
    Files.createDirectories(Path.of(temDirLocation));
    var fileLocation = temDirLocation + "marc.mrc";

    var writer = new LocalStorageWriter(fileLocation, OUTPUT_BUFFER_SIZE);

    writer.write(invalidData);
    writer.close();
    var file = new File(fileLocation);

    assertFalse(file.exists());

    FileUtils.deleteDirectory(new File(temDirLocation));
  }
}
