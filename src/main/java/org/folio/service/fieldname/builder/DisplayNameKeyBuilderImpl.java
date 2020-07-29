package org.folio.service.fieldname.builder;

import org.folio.rest.jaxrs.model.FieldName;
import org.springframework.stereotype.Service;

import java.util.StringJoiner;


@Service
public class DisplayNameKeyBuilderImpl implements DisplayNameKeyBuilder {

  public static final String DOT_DELIMITER = ".";

  @Override
  public String build(FieldName.RecordType recordType, String fieldName) {
    return new StringJoiner(DOT_DELIMITER)
      .add(recordType.toString().toLowerCase())
      .add(fieldName)
      .toString();
  }

}
