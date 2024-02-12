package org.folio.dataexp.repository;

import org.folio.dataexp.BaseDataExportInitializer;

import java.util.UUID;

public abstract class AllRepositoryTest extends BaseDataExportInitializer {

  protected static final UUID MIN_UUID = UUID.fromString("00000000-0000-0000-0000-00000000");
  protected static final UUID MAX_UUID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffff");
  protected static final int exportIdsBatch = 5000;
}
