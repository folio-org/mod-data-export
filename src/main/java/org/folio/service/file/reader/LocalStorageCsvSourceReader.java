package org.folio.service.file.reader;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.folio.rest.jaxrs.model.FileDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

import static java.util.Objects.nonNull;

@SuppressWarnings({"java:S2095"})
public class LocalStorageCsvSourceReader implements SourceReader {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Stream<String> fileStream;
  private Iterator<List<String>> iterator;

  @Override
  public void init(FileDefinition fileDefinition, int batchSize) {
    try{
      fileStream = Files.lines(Paths.get(fileDefinition.getSourcePath()));
      iterator = Iterables.partition(fileStream::iterator, batchSize).iterator();
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
    if(nonNull(fileStream)) {
      fileStream.close();
    }
  }
}
