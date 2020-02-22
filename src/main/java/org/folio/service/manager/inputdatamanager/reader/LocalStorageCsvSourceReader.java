package org.folio.service.manager.inputdatamanager.reader;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.folio.rest.jaxrs.model.FileDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;
import com.sun.xml.xsom.impl.scd.Iterators;

@Component
public class LocalStorageCsvSourceReader implements SourceReader {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public Iterator<List<String>> getSourceStream(FileDefinition fileDefinition, int batchSize) {
    try {
      Stream<String> lines = Files.lines(Paths.get(fileDefinition.getSourcePath()));
      return Iterables.partition(lines::iterator, batchSize).iterator();
    } catch (IOException e) {
      LOGGER.error("Exception while reading from {} ", fileDefinition.getFileName(), e);
    }
    return Iterators.empty();
  }
}
