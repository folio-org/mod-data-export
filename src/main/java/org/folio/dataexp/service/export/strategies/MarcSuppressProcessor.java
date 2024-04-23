package org.folio.dataexp.service.export.strategies;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.folio.dataexp.util.Constants.COMMA;

import org.folio.dataexp.domain.dto.MappingProfile;
import org.marc4j.marc.Record;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

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
        .filter(f -> "999".equals(f.getTag()) && 'f' == f.getIndicator1() && 'f' == f.getIndicator2())
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
}
