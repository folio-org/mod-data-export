package org.folio;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.marc4j.MarcJsonReader;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.marc.Record;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class TestUtil {
  public static String readFileContentFromResources(String path) {
    try {
      ClassLoader classLoader = TestUtil.class.getClassLoader();
      URL url = classLoader.getResource(path);
      return IOUtils.toString(url, StandardCharsets.UTF_8);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  public static File getFileFromResources(String path) {
    ClassLoader classLoader = TestUtil.class.getClassLoader();
    return new File(Objects.requireNonNull(classLoader.getResource(path)).getFile());
  }

  public static String readFileContent(String path) {
    try {
      return FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  /**
   * This method fetches the expected value from Json file and converts it into MARC
   *
   * @return expected Json converted to marc format
   * @throws FileNotFoundException
   */
  public static String getExpectedMarcFromJson(File expectedFile) throws FileNotFoundException {
    InputStream inputStream = new FileInputStream(expectedFile);
    MarcReader marcReader = new MarcJsonReader(inputStream);
    OutputStream outputStream = new ByteArrayOutputStream();
    MarcWriter writer = new MarcStreamWriter(outputStream);
    while (marcReader.hasNext()) {
      Record record = marcReader.next();
      writer.write(record);
    }

    writer.close();
    return outputStream.toString();
  }
}
