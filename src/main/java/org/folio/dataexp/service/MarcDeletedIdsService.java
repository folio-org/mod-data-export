package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.folio.dataexp.client.SourceStorageClient;
import org.folio.dataexp.domain.dto.MarcDeletedIdsCollection;
import org.folio.dataexp.domain.dto.MarcRecordIdentifiersPayload;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.folio.dataexp.util.Constants.DATE_PATTERN;

@Service
@RequiredArgsConstructor
@Log4j2
public class MarcDeletedIdsService {

  private final static String FIELD_SEARCH_EXPRESSION_TEMPLATE_FROM = "005.date from '%s'";
  private final static String FIELD_SEARCH_EXPRESSION_TEMPLATE_TO = "005.date to '%s'";
  private final static String FIELD_SEARCH_EXPRESSION_TEMPLATE_IN_RANGE = "005.date from '%s'-005.date to '%s'";
  private final static String LEADER_SEARCH_EXPRESSION_DELETED = "p_05 = 'd'";

  private final SourceStorageClient sourceStorageClient;
  private final ConsortiaService consortiaService;

  public MarcDeletedIdsCollection getMarcDeletedIds(Date from, Date to) {
    var marcDeletedIdsCollection = new MarcDeletedIdsCollection();
    var payload = new MarcRecordIdentifiersPayload()
      .withLeaderSearchExpression(LEADER_SEARCH_EXPRESSION_DELETED);
    enrichWithDate(payload, from, to);
    var marcIds = sourceStorageClient.getMarcRecordsIdentifiers(payload).getRecords().stream()
      .collect(Collectors.toSet()).stream().map(rec -> UUID.fromString(rec)).toList();
    marcDeletedIdsCollection.setDeletedMarcIds(marcIds);
    marcDeletedIdsCollection.setTotalRecords(marcIds.size());
    log.info("Found deleted MARC IDs: {}", marcDeletedIdsCollection.getDeletedMarcIds());

    // shared
    var centralTenantId = consortiaService.getCentralTenantId();
    if (StringUtils.isNotEmpty(centralTenantId)) {
      var marcIdsFromCentral = sourceStorageClient.getMarcRecordsIdentifiers(payload, centralTenantId).getRecords().stream()
        .collect(Collectors.toSet()).stream().map(rec -> UUID.fromString(rec)).toList();
      log.info("Found deleted MARC IDs from central tenant: {}", marcIdsFromCentral.size());
      marcDeletedIdsCollection.getDeletedMarcIds().addAll(marcIdsFromCentral);
      marcDeletedIdsCollection.setTotalRecords(marcDeletedIdsCollection.getTotalRecords() + marcIdsFromCentral.size());
    }

    return marcDeletedIdsCollection;
  }

  private void enrichWithDate(MarcRecordIdentifiersPayload payload, Date from, Date to) {
    if (nonNull(from) && isNull(to)) {
      payload.withFieldsSearchExpression(format(FIELD_SEARCH_EXPRESSION_TEMPLATE_FROM, DateFormatUtils.format(from, DATE_PATTERN)));
    } else if (nonNull(to) && isNull(from)) {
      payload.withFieldsSearchExpression(format(FIELD_SEARCH_EXPRESSION_TEMPLATE_TO, DateFormatUtils.format(to, DATE_PATTERN)));
    } else if (nonNull(from) && nonNull(to)) {
      payload.withFieldsSearchExpression(format(FIELD_SEARCH_EXPRESSION_TEMPLATE_IN_RANGE, DateFormatUtils.format(from, DATE_PATTERN),
        DateFormatUtils.format(to, DATE_PATTERN)));
    }
  }
}
