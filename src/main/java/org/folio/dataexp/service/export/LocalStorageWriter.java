package org.folio.dataexp.service.export;

import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.exception.export.LocalStorageWriterException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalStorageWriter extends StringWriter {
  private final File tmp;
  private final BufferedWriter writer;

  public LocalStorageWriter(String path, int size) {
    try {
      Path p = Path.of(path);
      this.tmp = Files.createFile(p)
        .toFile();
      this.writer = new BufferedWriter(new FileWriter(this.tmp), size);
    } catch (Exception ex) {
      throw new LocalStorageWriterException("Files buffer cannot be created due to error: " + ex.getMessage());
    }
  }

  @Override
  public void write(String data) {
    if (StringUtils.isNotEmpty(data)) {
      try {
        writer.append(data);
      } catch (IOException e) {
        deleteTmp(tmp);
      }
    } else {
      deleteTmp(tmp);
    }
  }

  @Override
  public void close() {
    try {
      if (tmp.exists()) {
        writer.close();
      }
    } catch (Exception ex) {
      throw new LocalStorageWriterException("Error while close(): " + ex.getMessage());
    }
  }

  private void deleteTmp(File tmp) {
    try {
      close();
      Files.deleteIfExists(tmp.toPath());
    } catch (IOException ex) {
      throw new LocalStorageWriterException("Error in deleting file: " + ex.getMessage());
    }
  }
}
