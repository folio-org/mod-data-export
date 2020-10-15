package org.folio.service.file.reader;

import com.google.common.collect.Iterables;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.FileDefinition;
import org.folio.service.logs.ErrorLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.folio.rest.jaxrs.model.FileDefinition.UploadFormat.CQL;

@SuppressWarnings({"java:S2095"})
public class LocalStorageCsvSourceReader implements SourceReader {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String UUID_PATTERN = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$";

  private FileDefinition fileDefinition;
  private Stream<String> fileStream;
  private Iterator<List<String>> iterator;
  private ErrorLogService errorLogService;
  private String jobExecutionId;
  private String tenantId;

  @Override
  public void init(FileDefinition fileDefinition, ErrorLogService errorLogService, String jobExecutionId, String tenantId, int batchSize) {
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
      .map(s -> s.replaceAll("\"", "").trim())
      .map(s -> s.replaceAll(",", "").trim())
      .filter(s -> s.matches(UUID_PATTERN))
      .collect(Collectors.toList());
  }

  @Override
  public void close() {
    if (nonNull(fileStream)) {
      fileStream.close();
    }
  }

  @Override
  public long totalCount() {
    if (nonNull(fileDefinition) && !CQL.equals(fileDefinition.getUploadFormat())) {
      try (Stream<String> fileLines = Files.lines(Paths.get(fileDefinition.getSourcePath()))) {
        return getValidUUIDsCountAndSaveErrorIfInvalidFound(fileLines);
      } catch (IOException e) {
        LOGGER.error(e.getMessage(), e);
      }
    }
    return 0L;
  }

  private long getValidUUIDsCountAndSaveErrorIfInvalidFound(Stream<String> fileLines) {
    List<String> invalidUUIDs = new ArrayList<>();
    long count = fileLines.
      filter(s -> {
        if (StringUtils.isNotEmpty(s) && s.replaceAll("[$,|\"]", "").trim().matches(UUID_PATTERN)) {
          return true;
        } else {
          invalidUUIDs.add(s);
          return false;
        }
      }).count();
    if (CollectionUtils.isNotEmpty(invalidUUIDs)) {
      errorLogService.saveGeneralError(format("Current UUIDs is in wrong format: %s", String.join(",", invalidUUIDs)), jobExecutionId, tenantId);
    }
    return count;
  }


}
