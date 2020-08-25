package org.folio.service.transformationfields;

import java.util.HashMap;
import java.util.Map;

public class MetadataParametersConstants {
  private static final Map<String, String> fixedLengthDataElement = new HashMap<>();
  private static final Map<String, String> nameFieldParameter = new HashMap<>();
  private static final Map<String, String> codeFieldParameter = new HashMap<>();

  static {
    fixedLengthDataElement.put("datesOfPublication", "$.instance.publication[*].dateOfPublication");
    fixedLengthDataElement.put("languages", "$.instance.languages");
    nameFieldParameter.put("field", "name");
    codeFieldParameter.put("field", "code");
  }

  private MetadataParametersConstants() {
  }

  public static Map<String, String> getFixedLengthDataElement() {
    return fixedLengthDataElement;
  }

  public static Map<String, String> getNameFieldParameter() {
      return nameFieldParameter;
  }

  public static Map<String, String> getCodeFieldParameter() {
    return codeFieldParameter;
  }

}
