package org.folio.dataexp.repository;

import java.util.UUID;
import org.folio.dataexp.BaseDataExportInitializerIT;

/**
 * Abstract base class for repository tests that provides common constants and initialization.
 *
 * <p>Defines minimum and maximum UUID values and a default export batch size for use in tests.
 */
public abstract class AllRepositoryBaseIT extends BaseDataExportInitializerIT {

  protected static final UUID MIN_UUID = UUID.fromString("00000000-0000-0000-0000-00000000");
  protected static final UUID MAX_UUID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffff");
  protected static final int exportIdsBatch = 5000;
}
