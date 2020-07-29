package org.folio.service.fieldname.loader;

import io.vertx.core.json.JsonObject;
import org.folio.util.OkapiConnectionParams;

import java.util.Map;

public interface ReferenceDataLoader {

  Map<String, JsonObject> load(OkapiConnectionParams okapiConnectionParams);
}
