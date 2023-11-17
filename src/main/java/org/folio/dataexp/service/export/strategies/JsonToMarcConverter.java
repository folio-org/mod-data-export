package org.folio.dataexp.service.export.strategies;

import lombok.extern.log4j.Log4j2;
import org.marc4j.MarcException;
import org.marc4j.MarcJsonReader;
import org.marc4j.MarcStreamWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Log4j2
@Component
public class JsonToMarcConverter {

  public String convertJsonRecordToMarcRecord(String jsonRecord) throws IOException {
    var byteArrayInputStream = new ByteArrayInputStream(jsonRecord.getBytes(StandardCharsets.UTF_8));
    var byteArrayOutputStream = new ByteArrayOutputStream();
    try (byteArrayInputStream; byteArrayOutputStream) {
      var marcJsonReader = new MarcJsonReader(byteArrayInputStream);
      var marcStreamWriter = new MarcStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8.name());
      try {
        while (marcJsonReader.hasNext()) {
          var record = marcJsonReader.next();
          marcStreamWriter.write(record);
        }
        // Handle unchecked json parse exception when parser encounters with control character or
        // any other unexpected data.
      } catch (Exception e) {
        log.error(e.getMessage());
        throw new MarcException(e.getMessage());
      }
      return byteArrayOutputStream.toString();
    } catch (IOException e) {
      log.error(e.getMessage());
      throw e;
    }
  }
}
