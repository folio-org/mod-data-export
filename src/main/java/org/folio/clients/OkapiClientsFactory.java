package org.folio.clients;

import org.folio.clients.impl.SourceRecordStorageClient;
import org.folio.util.OkapiConnectionParams;

/**
 * Non thread-safe factory for creating okapi clients.
 *
 */
public class OkapiClientsFactory {

  private final OkapiConnectionParams okapiConnectionParams;

  private SynchronousOkapiClient sourceRecordStorageClient;


  private OkapiClientsFactory(OkapiConnectionParams okapiConnectionParams) {
    this.okapiConnectionParams = okapiConnectionParams;
  }

  public static OkapiClientsFactory create(OkapiConnectionParams okapiConnectionParams) {
    return new OkapiClientsFactory(okapiConnectionParams);
  }

  public SynchronousOkapiClient getSourceRecordStorageClient() {
    if (sourceRecordStorageClient == null) {
      sourceRecordStorageClient = new SourceRecordStorageClient(okapiConnectionParams);
    }
    return sourceRecordStorageClient;
  }
}
