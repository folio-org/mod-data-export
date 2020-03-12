package org.folio.util;

import org.folio.rest.persist.Criteria.Criteria;

public class HelperUtils {

  private HelperUtils() {

  }

  /**
   * Builds criteria by which db result is filtered
   *
   * @param jsonbField - json key name
   * @param value - value corresponding to the key
   * @return - Criteria object
   */
  public static Criteria constructCriteria(String jsonbField, String value) {
    Criteria criteria = new Criteria();
    criteria.addField(jsonbField);
    criteria.setOperation("=");
    criteria.setVal(value);
    return criteria;
  }

}
