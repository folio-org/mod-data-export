package org.folio.service;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.folio.domain.reader.LocalStorageCsvSourceStreamReader;
import org.junit.Before;
import org.junit.Test;

public class LocalStorageCSVFileUploadServiceTest extends DataExportTestBase {

  public static final int BATCH_SIZE = 10;

  @Before
  public void setUp() {
    sourceStreamReader = new LocalStorageCsvSourceStreamReader("src/test/resources/22_uuids.csv");
    fileUploadService = new LocalStorageCSVFileUploadService(sourceStreamReader);
    recordLoaderService = new DefaultRecordLoaderService(fileUploadService);
  }

  @Test
  public void testUuidsStreamSplits22UuidsInto3BatchesOfSize10() throws IOException {
    Stream<List<String>> sourceStream = fileUploadService.getSourceStream(BATCH_SIZE);

    List<List<String>> batches = sourceStream.collect(Collectors.toList());
    assertThat(batches, hasSize(3));
    assertThat(batches.get(2).size(), is(2));


  }
}
