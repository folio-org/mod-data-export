package org.folio.dataexp.service.transformationfields;

import java.util.StringJoiner;
import org.folio.dataexp.domain.dto.RecordTypes;
import org.springframework.stereotype.Component;

/**
 * Builder for display name keys for transformation fields.
 */
@Component
public class DisplayNameKeyBuilder {

  /** Delimiter for display name keys. */
  public static final String DOT_DELIMITER = ".";

  /**
   * Builds a display name key from record type and field config ID.
   *
   * @param recordType the record type
   * @param fieldConfigId the field config ID
   * @return the display name key
   */
  public String build(RecordTypes recordType, String fieldConfigId) {
    return new StringJoiner(DOT_DELIMITER)
      .add(recordType.toString().toLowerCase())
      .add(fieldConfigId)
      .toString();
  }
}
