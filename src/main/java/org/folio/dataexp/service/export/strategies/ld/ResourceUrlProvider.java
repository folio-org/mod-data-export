package org.folio.dataexp.service.export.strategies.ld;

import java.util.function.LongFunction;
import lombok.RequiredArgsConstructor;
import org.folio.dataexp.service.BaseUrlService;
import org.springframework.stereotype.Service;

/**
 * Provide a means to supply the host portion of a resource URL. This is not stored in the graph,
 * and this class is named as it is so the export converter can find it and use it to generate URLs.
 */
@Service
@RequiredArgsConstructor
public class ResourceUrlProvider implements LongFunction<String> {

  private final BaseUrlService baseUrlService;

  private static final String URL_PATTERN = "%s/linked-data-editor/resources/%s";

  @Override
  public String apply(long id) {
    var baseUrl = normalizeBaseUrl(baseUrlService.getBaseUrl());
    return String.format(URL_PATTERN, baseUrl, id);
  }

  private String normalizeBaseUrl(String baseUrl) {
    var normalized = baseUrl;
    while (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }
}
