package org.folio.dataexp.service.export.strategies;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.folio.rdf4ld.service.Rdf4LdService;

@Log4j2
@Component
@RequiredArgsConstructor
public class LinkedDataConverter {
  private Rdf4LdService rdf4LdService;

  public ByteArrayOutputStream convertLdJsonToBibframe2Rdf(String ldJson) throws JsonProcessingException {
    return rdf4LdService.mapLdToBibframe2Rdf(ldJson, RDFFormat.RDFXML);
  }
}