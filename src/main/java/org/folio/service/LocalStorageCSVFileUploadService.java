package org.folio.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.folio.domain.reader.SourceStreamReader;

import com.google.common.collect.Iterables;

public class LocalStorageCSVFileUploadService implements FileUploadService {

  private final SourceStreamReader sourceStreamReader;

  public LocalStorageCSVFileUploadService(SourceStreamReader sourceStreamReader) {
    this.sourceStreamReader = sourceStreamReader;
  }

  @Override
  public Stream<List<String>> getSourceStream(int batchSize) throws IOException {
    Stream<String> sourceStream = sourceStreamReader.getSourceStream();
    Iterable<List<String>> partition = Iterables.partition(sourceStream::iterator, batchSize);
    return StreamSupport.stream(partition.spliterator(), false);
  }


}
