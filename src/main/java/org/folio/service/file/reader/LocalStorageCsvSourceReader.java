package org.folio.service.file.reader;

import com.google.common.collect.Iterables;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.logs.ErrorLogService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.rest.jaxrs.model.FileDefinition.UploadFormat.CQL;
import static org.folio.util.ErrorCode.INVALID_UUID_FORMAT;

@SuppressWarnings({"java:S2095"})
public class LocalStorageCsvSourceReader implements SourceReader {
  private static final Logger LOGGER = LogManager.getLogger(MethodHandles.lookup().lookupClass());
  private static final String COMMA = ",";
  private static final Pattern PATTERN = Pattern.compile("^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$");
  private static final String SPECIAL_CHARACTERS_REGEX = "[^a-zA-Z0-9\\-]";

  private FileDefinition fileDefinition;
  private Stream<String> fileStream;
  private Iterator<List<String>> iterator;
  private ErrorLogService errorLogService;
  private String jobExecutionId;
  private String tenantId;

  @Override
  public void init(FileDefinition fileDefinition, ErrorLogService errorLogService, String jobExecutionId, String tenantId, int batchSize) {
    if (Objects.isNull(fileDefinition.getSourcePath())) {
      this.iterator = Collections.emptyIterator();
      return;
    }
    try {
      this.fileDefinition = fileDefinition;
      this.errorLogService = errorLogService;
      this.jobExecutionId = jobExecutionId;
      this.tenantId = tenantId;
      this.fileStream = Files.lines(Paths.get(fileDefinition.getSourcePath()));
      this.iterator = Iterables.partition(fileStream::iterator, batchSize).iterator();
    } catch (IOException e) {
      LOGGER.error("Exception while reading from {} ", fileDefinition.getFileName(), e);
      iterator = Collections.emptyIterator();
    }
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }

  @Override
  public List<String> readNext() {
    return iterator.next()
      .stream()
      .map(s -> s.replaceAll(SPECIAL_CHARACTERS_REGEX, EMPTY).trim())
      .filter(s -> PATTERN.matcher(s).matches())
      .collect(Collectors.toList());
  }

  @Override
  public void close() {
    if (nonNull(fileStream)) {
      fileStream.close();
    }
  }

  @Override
  public int totalCount() {
    if (nonNull(fileDefinition) && !CQL.equals(fileDefinition.getUploadFormat())) {
      try (Stream<String> fileLines = Files.lines(Paths.get(fileDefinition.getSourcePath()))) {
        return getValidUUIDsCountAndSaveErrorIfInvalidFound(fileLines);
      } catch (IOException e) {
        LOGGER.error(e.getMessage(), e);
      }
    }
    return 0;
  }

  private int getValidUUIDsCountAndSaveErrorIfInvalidFound(Stream<String> fileLines) {
    List<String> invalidUUIDs = new ArrayList<>();
    long count = fileLines
      .filter(s -> {
        if (StringUtils.isNotEmpty(s) && PATTERN.matcher(s.replaceAll(SPECIAL_CHARACTERS_REGEX, EMPTY).trim()).matches()) {
          return true;
        } else {
          invalidUUIDs.add(s);
          return false;
        }
      }).count();
    if (CollectionUtils.isNotEmpty(invalidUUIDs)) {
      errorLogService.saveGeneralErrorWithMessageValues(INVALID_UUID_FORMAT.getCode(), Arrays.asList(String.join(COMMA, invalidUUIDs)), jobExecutionId, tenantId);
    }
    return (int) count;
  }


}
