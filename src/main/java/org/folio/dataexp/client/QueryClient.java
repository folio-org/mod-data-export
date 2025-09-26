package org.folio.dataexp.client;

import org.folio.querytool.domain.dto.QueryDetails;
import org.folio.querytool.domain.dto.QueryIdentifier;
import org.folio.querytool.domain.dto.SubmitQuery;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "query")
public interface QueryClient {
  @PostMapping("")
  QueryIdentifier executeQuery(@RequestBody SubmitQuery submitQuery);

  @GetMapping("/{queryId}")
  QueryDetails getQuery(@RequestHeader UUID queryId, @RequestParam Boolean includeResults);

  @GetMapping("/{queryId}?includeResults=true")
  QueryDetails getQuery(@RequestHeader UUID queryId, @RequestParam Integer offset, @RequestParam Integer limit);

  @GetMapping("/{queryId}/sortedIds")
  List<List<String>> getSortedIds(@RequestHeader UUID queryId, @RequestParam Integer offset, @RequestParam Integer limit);
}
