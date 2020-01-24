package org.folio.service;

import java.io.IOException;
import java.util.stream.Stream;

import org.folio.domain.Batch;
import org.folio.domain.records.FolioRecord;

/**
 * Streaming API for working with batches of Folio records
 *
 */
public interface RecordLoaderService {

  Stream<Batch<? extends FolioRecord>> getBatchRecordsStream(int batchSize) throws IOException;
}
