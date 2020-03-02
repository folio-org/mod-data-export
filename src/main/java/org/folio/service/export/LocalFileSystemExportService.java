package org.folio.service.export;

import com.google.common.primitives.Bytes;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.upload.storage.FileStorage;
import org.marc4j.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocalFileSystemExportService implements ExportService {
  private final byte[] MARC_RECORD_TERMINATOR = String.valueOf(Constants.RT).getBytes();

  @Autowired
  @Qualifier("LocalFileSystemStorage")
  private FileStorage fileStorage;

  @Override
  public void export(List<String> marcRecords, FileDefinition fileDefinition) {
    if (CollectionUtils.isNotEmpty(marcRecords) && fileDefinition != null) {
      for (String marcRecord : marcRecords) {
        byte[] rawRecord = convertToRawRecord(marcRecord);
        fileStorage.saveFileDataBlocking(rawRecord, fileDefinition);
      }
    }
  }

  private byte[] convertToRawRecord(String marcRecord) {
    /*
        MARC records may come in 3 formats: MARC_RAW, MARC_JSON, MARC_XML.
        We suppose to receive records only in MARC_RAW format
    */
    return Bytes.concat(marcRecord.getBytes(), MARC_RECORD_TERMINATOR);
  }

  @Override
  public void postExport(FileDefinition fileDefinition) {
    // copy file to S3
  }
}
