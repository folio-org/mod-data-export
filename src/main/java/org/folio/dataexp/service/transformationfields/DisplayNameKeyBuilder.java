package org.folio.dataexp.service.transformationfields;

import org.folio.dataexp.domain.dto.RecordTypes;
import org.springframework.stereotype.Component;

import java.util.StringJoiner;


@Component
public class DisplayNameKeyBuilder {

  public static final String DOT_DELIMITER = ".";

  public String build(RecordTypes recordType, String fieldConfigId) {
    return new StringJoiner(DOT_DELIMITER)
      .add(recordType.toString().toLowerCase())
      .add(fieldConfigId)
      .toString();
  }

}
