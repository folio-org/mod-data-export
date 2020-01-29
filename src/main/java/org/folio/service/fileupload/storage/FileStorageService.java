package org.folio.service.fileupload.storage;

import org.folio.service.fileupload.reader.SourceStreamReader;

/**
 * File storage service.
 * Service to save files and reading files being stored.
 */
public interface FileStorageService {

  /**
   * Returns instance of the source reader.
   *
   * @return SourceStreamReader
   * @see SourceStreamReader
   */
   SourceStreamReader getReader();
}
