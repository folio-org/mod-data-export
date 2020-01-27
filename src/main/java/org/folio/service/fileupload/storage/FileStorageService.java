package org.folio.service.fileupload.storage;

import org.folio.service.fileupload.reader.SourceStreamReader;

/**
 * File storage service.
 * Service to save files and reading files being stored.
 */
public interface FileStorageService {

  /**
   * Returns an instance of the source reader.
   * @return SourceStreamReader
   * @see SourceStreamReader
   */
  default SourceStreamReader getReader() {
    return new SourceStreamReader() {
    };
  }
}
