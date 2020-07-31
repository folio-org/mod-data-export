package org.folio.service.fieldname.builder;

import org.folio.rest.jaxrs.model.TransformationField.RecordType;
import org.springframework.stereotype.Component;

import java.util.StringJoiner;


@Component
public class DisplayNameKeyBuilderImpl implements DisplayNameKeyBuilder {

  public static final String DOT_DELIMITER = ".";

  @Override
  public String build(RecordType recordType, String fieldConfigId) {
    return new StringJoiner(DOT_DELIMITER)
      .add(recordType.toString().toLowerCase())
      .add(fieldConfigId)
      .toString();
  }

}
