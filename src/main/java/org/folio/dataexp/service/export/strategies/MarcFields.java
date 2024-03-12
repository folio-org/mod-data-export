package org.folio.dataexp.service.export.strategies;

import lombok.Getter;
import lombok.Setter;
import org.marc4j.marc.VariableField;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MarcFields {
  private List<VariableField> holdingItemsFields = new ArrayList<>();
  private List<String> errorMessages = new ArrayList<>();
}
