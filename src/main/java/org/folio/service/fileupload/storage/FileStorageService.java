package org.folio.service.fileupload.storage;

import org.folio.service.fileupload.reader.SourceStreamReader;

public interface FileStorageService {

  default SourceStreamReader getSourceReader() {
    return new SourceStreamReader() {};
  }
}
