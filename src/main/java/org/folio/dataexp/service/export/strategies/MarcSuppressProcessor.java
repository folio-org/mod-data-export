package org.folio.dataexp.service.export.strategies;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.folio.dataexp.util.Constants.COMMA;

import lombok.extern.log4j.Log4j2;
import org.folio.dataexp.domain.dto.MappingProfile;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class MarcSuppressProcessor {
  private final Set<String> fieldsToSuppress;
  private final boolean suppress999ff;

  public MarcSuppressProcessor(MappingProfile mappingProfile) {
    fieldsToSuppress = isEmpty(mappingProfile.getFieldsSuppression()) ?
      Collections.emptySet() :
      Arrays.stream(mappingProfile.getFieldsSuppression().split(COMMA))
        .map(String::trim)
        .collect(Collectors.toSet());
    suppress999ff = Boolean.TRUE.equals(mappingProfile.getSuppress999ff());
  }

  public Record suppress(Record rec) {
    if (suppress999ff) {
      var records = rec.getDataFields().stream()
        .filter(this::shouldSuppress999ff)
        .toList();
      records.forEach(rec::removeVariableField);
    }
    if (isNotEmpty(fieldsToSuppress)) {
      var records = rec.getVariableFields().stream()
        .filter(f -> fieldsToSuppress.contains(f.getTag()))
        .toList();
      records.forEach(rec::removeVariableField);
    }
    return rec;
  }

  private boolean shouldSuppress999ff(DataField dataField) {
    return "999".equals(dataField.getTag()) && 'f' == dataField.getIndicator1() && 'f' == dataField.getIndicator2();
  }
}
