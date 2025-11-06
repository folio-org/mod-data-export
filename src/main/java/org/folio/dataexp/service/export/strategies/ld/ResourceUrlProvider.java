package org.folio.dataexp.service.export.strategies.ld;

import java.util.function.LongFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Provide a means to supply the host portion of a resource URL.
 * This is not stored in the graph, and this class is named as it
 * is so the export converter can find it and use it to generate URLs.
 */
@Service
@RequiredArgsConstructor
public class ResourceUrlProvider implements LongFunction<String> {

  private static final String URL_PATTERN = "%s/linked-data-editor/resources/%s";

  @Override
  public String apply(long id) {
    // TODO: MDEXP-873 - replace placeholder with a mod-settings based implementation
    return String.format(URL_PATTERN, "http://localhost", id);
  }
}
