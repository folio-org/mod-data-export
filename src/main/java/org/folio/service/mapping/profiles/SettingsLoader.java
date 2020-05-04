package org.folio.service.mapping.profiles;

import io.vertx.core.json.JsonObject;
import org.folio.util.OkapiConnectionParams;

import java.util.Map;

public interface SettingsLoader {

  Map<String, JsonObject> load(OkapiConnectionParams okapiConnectionParams);

}
