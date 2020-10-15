package org.folio.service.file.reader;

import static java.util.Objects.nonNull;
import static org.folio.rest.jaxrs.model.FileDefinition.UploadFormat.CQL;

import com.google.common.collect.Iterables;

import org.folio.rest.jaxrs.model.FileDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"java:S2095"})
public class LocalStorageCsvSourceReader implements SourceReader {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private FileDefinition fileDefinition;
  private Stream<String> fileStream;
  private Iterator<List<String>> iterator;

  @Override
  public void init(FileDefinition fileDefinition, int batchSize) {
    if (Objects.isNull(fileDefinition.getSourcePath())) {
      this.iterator = Collections.emptyIterator();
      return;
    }

    try {
      this.fileDefinition = fileDefinition;
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
        return fileLines.count();
      } catch (IOException e) {
        LOGGER.error(e.getMessage(), e);
      }
    }
    return 0L;
  }


}
