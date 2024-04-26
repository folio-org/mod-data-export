package org.folio.dataexp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.folio.dataexp.client.SourceStorageClient;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.MarcRecordIdentifiersPayload;
import org.folio.dataexp.exception.export.ExportDeletedDateRangeException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.folio.dataexp.util.Constants.DATE_PATTERN;

@Service
@RequiredArgsConstructor
@Log4j2
public class MarcDeletedIdsService {

  private static final String FIELD_SEARCH_EXPRESSION_TEMPLATE_FROM = "005.date from '%s'";
  private static final String FIELD_SEARCH_EXPRESSION_TEMPLATE_TO = "005.date to '%s'";
  private static final String FIELD_SEARCH_EXPRESSION_TEMPLATE_IN_RANGE = "005.date in '%s-%s'";
  private static final String LEADER_SEARCH_EXPRESSION_DELETED = "p_05 = 'd'";

  private final SourceStorageClient sourceStorageClient;
  private final FileDefinitionsService fileDefinitionsService;

  public FileDefinition getFileDefinitionForMarcDeletedIds(Date from, Date to) {
    var dateFrom = nonNull(from) ? from.toInstant() : null;
    var dateTo = nonNull(to) ? to.toInstant() : null;
    log.info("GET MARC deleted IDs with date from {}, date to {}", dateFrom, dateTo);
    validateDates(from, to);
    var payload = new MarcRecordIdentifiersPayload().withLeaderSearchExpression(LEADER_SEARCH_EXPRESSION_DELETED);
    enrichWithDate(payload, from, to);

    List<String> marcIds = sourceStorageClient.getMarcRecordsIdentifiers(payload).getRecords();
    log.info("Found deleted MARC IDs: {}", marcIds.size());
    var fileContent = String.join(System.lineSeparator(), marcIds);

    var fileDefinition = new FileDefinition();
    fileDefinition.setSize(marcIds.size());
    fileDefinition.setUploadFormat(FileDefinition.UploadFormatEnum.CSV);
    fileDefinition.setFileName("marcDeletedIds.csv");
    fileDefinition = fileDefinitionsService.postFileDefinition(fileDefinition);
    fileDefinition = fileDefinitionsService.uploadFile(fileDefinition.getId(), new ByteArrayResource(fileContent.getBytes()));
    return fileDefinition;
  }

  private void enrichWithDate(MarcRecordIdentifiersPayload payload, Date from, Date to) {
    String searchExpression = null;

    if (nonNull(from)) {
      if (nonNull(to)) {
        searchExpression = format(FIELD_SEARCH_EXPRESSION_TEMPLATE_IN_RANGE, DateFormatUtils.format(from, DATE_PATTERN),
          DateFormatUtils.format(to, DATE_PATTERN));
      } else {
        searchExpression = format(FIELD_SEARCH_EXPRESSION_TEMPLATE_FROM, DateFormatUtils.format(from, DATE_PATTERN));
      }
    } else if (nonNull(to)) {
      searchExpression = format(FIELD_SEARCH_EXPRESSION_TEMPLATE_TO, DateFormatUtils.format(to, DATE_PATTERN));
    }

    payload.setFieldsSearchExpression(searchExpression);
  }

  private void validateDates(Date from, Date until) {
    if (nonNull(from) && nonNull(until)) {
      if (from.toInstant().isAfter(until.toInstant())) {
        throw new ExportDeletedDateRangeException("Invalid date range for payload: date 'from' cannot be after date 'to'.");
      }
    }
  }
}
