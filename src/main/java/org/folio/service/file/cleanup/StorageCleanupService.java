package org.folio.service.file.cleanup;

import io.vertx.core.Future;
import org.folio.util.OkapiConnectionParams;

public interface StorageCleanupService {

  /**
   * Cleans storage from files linked to completed fileDefinition or which have not been updated for {@code N} ms.
   *
   * @param params Okapi connection params
   * @return Future with true if files were deleted and false in otherwise.
   */
  Future<Boolean> cleanStorage(OkapiConnectionParams params);
}
