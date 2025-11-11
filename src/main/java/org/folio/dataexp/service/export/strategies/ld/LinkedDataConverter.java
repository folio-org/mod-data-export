package org.folio.dataexp.service.export.strategies.ld;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.folio.rdf4ld.service.Rdf4LdService;
import org.springframework.stereotype.Component;

/**
 * Converter for Linked Data resources.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class LinkedDataConverter {
  private final Rdf4LdService rdf4LdService;

  /**
   * Convert Linked Data exported resource JSON to BIBFRAME 2 vocabulary JSON-LD format.
   *
   * @param ldJson Linked Data exported resource JSON as a string
   * @return BIBFRAME 2 NDJSON-LD
   * @throws JsonProcessingException when input is not valid JSON
   */
  public ByteArrayOutputStream convertLdJsonToBibframe2Rdf(String ldJson)
      throws JsonProcessingException {
    var outputConfig = new WriterConfig();
    outputConfig.set(BasicWriterSettings.PRETTY_PRINT, false);
    return rdf4LdService.mapLdToBibframe2Rdf(ldJson, RDFFormat.JSONLD, outputConfig);
  }
}
