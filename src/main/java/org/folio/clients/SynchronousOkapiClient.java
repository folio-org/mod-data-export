package org.folio.clients;

import java.util.Optional;

import org.folio.rest.tools.client.Response;
import org.folio.util.OkapiConnectionParams;

import io.vertx.core.json.JsonObject;

/**
 * Base class for okapi clients that block the calling thread.
 */
public abstract class SynchronousOkapiClient {

  protected final OkapiConnectionParams okapiConnectionParams;

  public SynchronousOkapiClient(OkapiConnectionParams okapiConnectionParams) {
    this.okapiConnectionParams = okapiConnectionParams;
  }

  public abstract Optional<JsonObject>  getById(String id);

}
