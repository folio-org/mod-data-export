package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.folio.dataexp.client.SourceStorageClient;
import org.folio.dataexp.domain.dto.MarcDeletedIdsCollection;
import org.folio.dataexp.domain.dto.MarcRecordIdentifiersPayload;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
  private final static String FIELD_SEARCH_EXPRESSION_TEMPLATE_IN_RANGE = "005.date in '%s-%s'";
  private final static String LEADER_SEARCH_EXPRESSION_DELETED = "p_05 = 'd'";

  private final SourceStorageClient sourceStorageClient;
  private final ConsortiaService consortiaService;
  private final FolioExecutionContext folioExecutionContext;
  private final FolioModuleMetadata folioModuleMetadata;

  public MarcDeletedIdsCollection getMarcDeletedIds(Date from, Date to) {
    var marcDeletedIdsCollection = new MarcDeletedIdsCollection();
    var payload = new MarcRecordIdentifiersPayload().withLeaderSearchExpression(LEADER_SEARCH_EXPRESSION_DELETED);
    enrichWithDate(payload, from, to);

    List<UUID> marcIds = new ArrayList<>();
    marcIds.addAll(fetchFromLocalTenant(payload, marcDeletedIdsCollection));
    marcIds.addAll(fetchFromCentralTenant(payload, marcDeletedIdsCollection));

    marcDeletedIdsCollection.setDeletedMarcIds(marcIds);
    marcDeletedIdsCollection.setTotalRecords(marcIds.size());

    return marcDeletedIdsCollection;
  }

  private List<UUID> fetchFromLocalTenant(MarcRecordIdentifiersPayload payload, MarcDeletedIdsCollection marcDeletedIdsCollection) {
    var marcIds = sourceStorageClient.getMarcRecordsIdentifiers(payload).getRecords().stream()
      .collect(Collectors.toSet()).stream().map(rec -> UUID.fromString(rec)).toList();
    log.info("Found deleted MARC IDs from member tenant: {}", marcDeletedIdsCollection.getDeletedMarcIds());
    return marcIds;
  }

  private List<UUID> fetchFromCentralTenant(MarcRecordIdentifiersPayload payload, MarcDeletedIdsCollection marcDeletedIdsCollection) {
    var centralTenantId = consortiaService.getCentralTenantId();
    if (StringUtils.isNotEmpty(centralTenantId) && !centralTenantId.equals(folioExecutionContext.getTenantId())) {
      try (var ignored = new FolioExecutionContextSetter(prepareContextForTenant(centralTenantId, folioModuleMetadata, folioExecutionContext))) {
        var marcIdsFromCentral = sourceStorageClient.getMarcRecordsIdentifiers(payload).getRecords().stream()
          .collect(Collectors.toSet()).stream().map(rec -> UUID.fromString(rec)).toList();
        log.info("Found deleted MARC IDs from central tenant: {}", marcIdsFromCentral.size());
        return marcIdsFromCentral;
      }
    }
    return Collections.emptyList();
  }

  private void enrichWithDate(MarcRecordIdentifiersPayload payload, Date from, Date to) {
    if (nonNull(from) && isNull(to)) {
      payload.setFieldsSearchExpression(format(FIELD_SEARCH_EXPRESSION_TEMPLATE_FROM, DateFormatUtils.format(from, DATE_PATTERN)));
    } else if (nonNull(to) && isNull(from)) {
      payload.setFieldsSearchExpression(format(FIELD_SEARCH_EXPRESSION_TEMPLATE_TO, DateFormatUtils.format(to, DATE_PATTERN)));
    } else if (nonNull(from) && nonNull(to)) {
      payload.setFieldsSearchExpression(format(FIELD_SEARCH_EXPRESSION_TEMPLATE_IN_RANGE, DateFormatUtils.format(from, DATE_PATTERN),
        DateFormatUtils.format(to, DATE_PATTERN)));
    }
  }

  public static FolioExecutionContext prepareContextForTenant(String tenantId, FolioModuleMetadata folioModuleMetadata, FolioExecutionContext context) {
    if (MapUtils.isNotEmpty(context.getOkapiHeaders())) {
      // create deep copy of headers in order to make switching context thread safe
      var headersCopy = SerializationUtils.clone((HashMap<String, Collection<String>>) context.getAllHeaders());
      headersCopy.put(XOkapiHeaders.TENANT, List.of(tenantId));
      log.info("FOLIO context initialized with tenant {}", tenantId);
      return new DefaultFolioExecutionContext(folioModuleMetadata, headersCopy);
    }
    throw new IllegalStateException("Okapi headers not provided");
  }

  public static void runInFolioContext(FolioExecutionContext context, Runnable runnable) {
    try (var fec = new FolioExecutionContextSetter(context)) {
      runnable.run();
    }
  }
}
