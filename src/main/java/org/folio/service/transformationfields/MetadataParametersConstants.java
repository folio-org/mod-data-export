package org.folio.service.transformationfields;

import java.util.HashMap;
import java.util.Map;

public class MetadataParametersConstants {
  private static final Map<String, String> fixedLengthDataElement = new HashMap<>();

  static {
    fixedLengthDataElement.put("datesOfPublication", "$.instance.publication[*].dateOfPublication");
    fixedLengthDataElement.put("languages", "$.instance.languages");
  }

  private MetadataParametersConstants() {
  }

  public static Map<String, String> getFixedLengthDataElement() {
    return fixedLengthDataElement;
  }

}
