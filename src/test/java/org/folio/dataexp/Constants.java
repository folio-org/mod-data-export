package org.folio.dataexp;

import java.util.UUID;

/** Constants used for export tests. */
public class Constants {

  /** Default job profile ID for holdings exports. */
  public static final UUID DEFAULT_HOLDINGS_JOB_PROFILE =
      UUID.fromString("5e9835fc-0e51-44c8-8a47-f7b8fce35da7");

  /** Default job profile ID for authority exports. */
  public static final UUID DEFAULT_AUTHORITY_JOB_PROFILE =
      UUID.fromString("56944b1c-f3f9-475b-bed0-7387c33620ce");

  /** Default job profile ID for deleted authority exports. */
  public static final UUID DEFAULT_DELETED_AUTHORITY_JOB_PROFILE =
      UUID.fromString("2c9be114-6d35-4408-adac-9ead35f51a27");
}
