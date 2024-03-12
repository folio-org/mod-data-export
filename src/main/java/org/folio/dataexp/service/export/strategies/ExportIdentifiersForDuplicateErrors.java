package org.folio.dataexp.service.export.strategies;

import lombok.Getter;
import lombok.Setter;
import net.minidev.json.JSONObject;

@Setter
@Getter
public class ExportIdentifiersForDuplicateErrors {

  private String identifierHridMessage;
  private JSONObject associatedJsonObject;

}
