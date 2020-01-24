package org.folio.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;


public interface FileUploadService {

  Stream<List<String>> getSourceStream(int batchSize) throws IOException;

}
