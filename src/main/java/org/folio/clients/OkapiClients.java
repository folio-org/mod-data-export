package org.folio.clients;

import org.folio.clients.impl.SourceRecordStorageClient;
import org.folio.util.OkapiConnectionParams;

public class OkapiClients {

  private final OkapiConnectionParams okapiConnectionParams;

  private SynchronousOkapiClient sourceRecordStorageClient;


  private OkapiClients(OkapiConnectionParams okapiConnectionParams) {
    this.okapiConnectionParams = okapiConnectionParams;
  }

  public static OkapiClients create(OkapiConnectionParams okapiConnectionParams) {
    return new OkapiClients(okapiConnectionParams);
  }

  public SynchronousOkapiClient getSourceRecordStorageClient() {
    if (sourceRecordStorageClient == null) {
      sourceRecordStorageClient = new SourceRecordStorageClient(okapiConnectionParams);
    }
    return sourceRecordStorageClient;
  }
}
