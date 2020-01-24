package org.folio.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.folio.domain.Batch;
import org.folio.domain.records.FolioRecord;
import org.folio.domain.records.Instance;

public class DefaultRecordLoaderService implements RecordLoaderService {


  private final FileUploadService fileUploadService;

  public DefaultRecordLoaderService(FileUploadService fileUploadService) {
    this.fileUploadService = fileUploadService;
  }

  @Override
  public Stream<Batch<? extends FolioRecord>> getBatchRecordsStream(int batchSize) throws IOException {
    Stream<List<String>> sourceStream = fileUploadService.getSourceStream(batchSize);
    return sourceStream
      .map(this::callSrs)
      .map(this::callInventory)
      .map(this::mapToFolioRecords);
  }

  /**
   * TODO
   *
   * @param uuids
   * @return
   */
  private List<String> callInventory(List<String> uuids) {
    return uuids;
  }

  /**
   * TODO
   *
   * @param uuids
   * @return
   */
  private List<String> callSrs(List<String> uuids) {
    return uuids;
  }

  private Batch<? extends FolioRecord> mapToFolioRecords(List<String> uuids) {
    Collection<Instance> collect = uuids.stream().map(Instance::new).collect(Collectors.toCollection(ArrayList::new));
    return new Batch<>(collect);
  }
}
