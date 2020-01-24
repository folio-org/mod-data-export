package org.folio.service;


import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.folio.domain.Batch;
import org.folio.domain.reader.LocalStorageCsvSourceStreamReader;
import org.folio.domain.records.FolioRecord;
import org.junit.Before;
import org.junit.Test;

public class RecordLoaderServiceTest extends DataExportTestBase {

  private final static int BATCH_SIZE = 10;

  @Before
  public void setUp() {
    sourceStreamReader = new LocalStorageCsvSourceStreamReader("src/test/resources/22_uuids.csv");
    fileUploadService = new LocalStorageCSVFileUploadService(sourceStreamReader);
    recordLoaderService = new DefaultRecordLoaderService(fileUploadService);
  }

  @Test
  public void testFolioRecordsStreamSplitsFolioRecordsFrom22UuidsInto3BatchesOfSize10() throws IOException {
    Stream<Batch<? extends FolioRecord>> batchRecordsStream = recordLoaderService.getBatchRecordsStream(BATCH_SIZE);
    List<Batch<? extends FolioRecord>> batches = batchRecordsStream.collect(Collectors.toList());
    assertThat(batches, hasSize(3));
    assertThat(batches.get(2).getData().size(), is(2));
  }

}
