package org.folio.dataexp.service;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.folio.dataexp.util.Constants.DATE_PATTERN;
import static org.folio.dataexp.util.Constants.DELETED_MARC_IDS_FILE_NAME;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.folio.dataexp.client.SourceStorageClient;
import org.folio.dataexp.domain.dto.FileDefinition;
import org.folio.dataexp.domain.dto.MarcRecordIdentifiersPayload;
import org.folio.dataexp.exception.export.ExportDeletedDateRangeException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

/** Service for retrieving deleted MARC record IDs and creating file definitions. */
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

  /**
   * Gets a FileDefinition for deleted MARC IDs within a date range.
   *
   * @param from Start date.
   * @param to End date.
   * @return The FileDefinition containing deleted MARC IDs.
   */
  public FileDefinition getFileDefinitionForMarcDeletedIds(Date from, Date to) {
    validateDates(from, to);
    if (isNull(from) && isNull(to)) {
      Date now = new Date();
      Date previousDay =
          Date.from(
              LocalDateTime.ofInstant(now.toInstant(), ZoneId.of("UTC"))
                  .minusDays(1)
                  .atZone(ZoneId.of("UTC"))
                  .toInstant());
      from = previousDay;
      to = previousDay;
      log.info("The previous day is used: {}", from.toInstant());
    }
    log.info("GET MARC deleted IDs with date from {}, date to {}", from, to);
    var payload =
        new MarcRecordIdentifiersPayload()
            .withLeaderSearchExpression(LEADER_SEARCH_EXPRESSION_DELETED);
    enrichWithDate(payload, from, to);

    List<String> marcIds = sourceStorageClient.getMarcRecordsIdentifiers(payload).getRecords();
    log.info("Found deleted MARC IDs: {}", marcIds.size());

    var fileDefinition = new FileDefinition();
    fileDefinition.setSize(marcIds.size());
    fileDefinition.setUploadFormat(FileDefinition.UploadFormatEnum.CSV);
    fileDefinition.setFileName(DELETED_MARC_IDS_FILE_NAME);
    fileDefinition = fileDefinitionsService.postFileDefinition(fileDefinition);
    var fileContent = String.join(System.lineSeparator(), marcIds);
    fileDefinition =
        fileDefinitionsService.uploadFile(
            fileDefinition.getId(), new ByteArrayResource(fileContent.getBytes()));
    return fileDefinition;
  }

  /**
   * Enriches the payload with date search expressions.
   *
   * @param payload The MarcRecordIdentifiersPayload.
   * @param from Start date.
   * @param to End date.
   */
  private void enrichWithDate(MarcRecordIdentifiersPayload payload, Date from, Date to) {
    String searchExpression = null;

    if (nonNull(from)) {
      if (nonNull(to)) {
        searchExpression =
            format(
                FIELD_SEARCH_EXPRESSION_TEMPLATE_IN_RANGE,
                DateFormatUtils.format(from, DATE_PATTERN),
                DateFormatUtils.format(to, DATE_PATTERN));
      } else {
        searchExpression =
            format(
                FIELD_SEARCH_EXPRESSION_TEMPLATE_FROM, DateFormatUtils.format(from, DATE_PATTERN));
      }
    } else if (nonNull(to)) {
      searchExpression =
          format(FIELD_SEARCH_EXPRESSION_TEMPLATE_TO, DateFormatUtils.format(to, DATE_PATTERN));
    }

    payload.setFieldsSearchExpression(searchExpression);
  }

  /**
   * Validates the date range for deleted MARC IDs.
   *
   * @param from Start date.
   * @param until End date.
   * @throws ExportDeletedDateRangeException if the date range is invalid.
   */
  private void validateDates(Date from, Date until) {
    if (nonNull(from) && nonNull(until) && from.toInstant().isAfter(until.toInstant())) {
      throw new ExportDeletedDateRangeException(
          "Invalid date range for payload: date 'from' cannot be after date 'to'.");
    }
  }
}
