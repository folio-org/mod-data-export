package org.folio.dataexp.service.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.lang3.StringUtils;
import org.folio.dataexp.exception.export.LocalStorageWriterException;

/**
 * Writer for local storage files, used for temporary export file creation.
 */
public class LocalStorageWriter extends StringWriter {
  private final File tmp;
  private final BufferedWriter writer;

  /**
   * Constructs a LocalStorageWriter for the given path and buffer size.
   *
   * @param path the file path
   * @param size the buffer size
   */
  public LocalStorageWriter(String path, int size) {
    try {
      Path p = Path.of(path);
      this.tmp = Files.createFile(p)
          .toFile();
      this.writer = new BufferedWriter(new FileWriter(this.tmp), size);
    } catch (Exception ex) {
      throw new LocalStorageWriterException(
          "Files buffer cannot be created due to error: " + ex.getMessage());
    }
  }

  /**
   * Writes data to the file.
   *
   * @param data the data to write
   */
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

  /**
   * Closes the writer and file.
   */
  @Override
  public void close() {
    try {
      if (tmp.exists()) {
        writer.close();
      }
    } catch (Exception ex) {
      throw new LocalStorageWriterException(
          "Error while close(): " + ex.getMessage());
    }
  }

  /**
   * Deletes the temporary file.
   *
   * @param tmp the file to delete
   */
  private void deleteTmp(File tmp) {
    try {
      close();
      Files.deleteIfExists(tmp.toPath());
    } catch (IOException ex) {
      throw new LocalStorageWriterException(
          "Error in deleting file: " + ex.getMessage());
    }
  }
}
