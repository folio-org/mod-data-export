package org.folio.dataexp.service.export.strategies;

import lombok.Getter;
import lombok.Setter;
import net.minidev.json.JSONObject;

/** Holds identifiers and associated JSON object for duplicate error reporting. */
@Setter
@Getter
public class ExportIdentifiersForDuplicateError {

  private String identifierHridMessage;
  private JSONObject associatedJsonObject;
}
