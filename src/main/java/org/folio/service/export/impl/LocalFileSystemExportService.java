package org.folio.service.export.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.export.ExportService;
import org.folio.service.upload.storage.FileStorage;
import org.marc4j.MarcJsonReader;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.marc.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class LocalFileSystemExportService implements ExportService {
  @Autowired
  @Qualifier("LocalFileSystemStorage")
  private FileStorage fileStorage;

  @Override
  public void export(List<String> jsonRecords, FileDefinition fileDefinition) {
    if (CollectionUtils.isNotEmpty(jsonRecords) && fileDefinition != null) {
      for (String jsonRecord : jsonRecords) {
        byte[] marcRecord = convertJsonRecordToMarcRecord(jsonRecord);
        fileStorage.saveFileDataBlocking(marcRecord, fileDefinition);
      }
    }
  }

  /**
   * Converts incoming marc record from json format to raw format
   * @param jsonRecord json record
   * @return  array of bytes
   */
  private byte[] convertJsonRecordToMarcRecord(String jsonRecord) {
    MarcReader marcJsonReader = new MarcJsonReader(new ByteArrayInputStream(jsonRecord.getBytes(StandardCharsets.UTF_8)));
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    MarcWriter marcStreamWriter = new MarcStreamWriter(byteArrayOutputStream);
    while (marcJsonReader.hasNext()) {
      Record record = marcJsonReader.next();
      marcStreamWriter.write(record);
    }
    return byteArrayOutputStream.toByteArray();
  }

  @Override
  public void postExport(FileDefinition fileDefinition) {
    // copy file to S3
  }
}
