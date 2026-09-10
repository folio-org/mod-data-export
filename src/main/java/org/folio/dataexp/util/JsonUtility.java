package org.folio.dataexp.util;

import static org.folio.dataexp.service.export.Constants.DELETED_KEY;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import net.minidev.json.JSONObject;

/** JSON utility methods shared across export components. */
@UtilityClass
@Log4j2
public class JsonUtility {

  /**
   * Returns true if the instance JSON has a truthy "deleted" field.
   * Handles both Boolean and String values; logs a warning for unexpected types.
   */
  public static boolean isDeleted(JSONObject instance) {
    try {
      return instance.containsKey(DELETED_KEY) && (boolean) instance.get(DELETED_KEY);
    } catch (ClassCastException e) {
      log.warn("isDeleted:: unexpected type for '{}' field: {}",
          DELETED_KEY, instance.get(DELETED_KEY).getClass().getSimpleName());
      return false;
    }
  }
}
