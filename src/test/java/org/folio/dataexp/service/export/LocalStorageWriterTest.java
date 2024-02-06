package org.folio.dataexp.service.export;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.folio.dataexp.service.export.Constants.OUTPUT_BUFFER_SIZE;
import static org.folio.dataexp.util.Constants.TEMP_DIR_FOR_EXPORTS_BY_JOB_EXECUTION_ID;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalStorageWriterTest {

  @Test
  @SneakyThrows
  void writeTest() {
    var jobExecutionId = UUID.randomUUID();
    var temDirLocation  = String.format(TEMP_DIR_FOR_EXPORTS_BY_JOB_EXECUTION_ID, jobExecutionId);
    Files.createDirectories(Path.of(temDirLocation));
    var fileLocation = temDirLocation + "marc.mrc";

    var writer =  new LocalStorageWriter(fileLocation, OUTPUT_BUFFER_SIZE);
    writer.write("data");
    writer.close();
    var file = new File(fileLocation);
    assertTrue(file.length() > 0);

    FileUtils.deleteDirectory(new File(temDirLocation));
  }
}
